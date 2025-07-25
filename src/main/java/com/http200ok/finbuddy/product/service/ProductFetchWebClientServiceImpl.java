package com.http200ok.finbuddy.product.service;

import com.http200ok.finbuddy.bank.domain.Bank;
import com.http200ok.finbuddy.bank.repository.BankRepository;
import com.http200ok.finbuddy.product.domain.DepositProduct;
import com.http200ok.finbuddy.product.domain.DepositProductOption;
import com.http200ok.finbuddy.product.domain.ProductType;
import com.http200ok.finbuddy.product.repository.DepositProductRepository;
import com.http200ok.finbuddy.product.repository.SavingProductRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProductFetchWebClientServiceImpl implements ProductFetchWebClientService{

    private final DepositProductRepository depositProductRepository;
    private final SavingProductRepository savingProductRepository;
    private final BankRepository bankRepository;
    private final WebClient webClient;

    private static final String BASE_URL = "http://finlife.fss.or.kr/finlifeapi/";
    @Value("${bank.api.key}")
    private String BANK_API_KEY;

    @Value("${bank.api.timeout}")
    private int timeoutSeconds;

    @Value("${bank.api.retry.max-attempts}")
    private int maxRetryAttempts;

    @Value("${bank.api.retry.delay-seconds}")
    private int retryDelaySeconds;

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    @Override
    public Mono<Void> fetchAndSaveProducts(String productTypeStr) {
        ProductType productType = ProductType.from(productTypeStr);
        String uri = buildUri(productType);

        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .retryWhen(Retry.backoff(maxRetryAttempts, Duration.ofSeconds(retryDelaySeconds))
                        .doBeforeRetry(retrySignal -> log.warn("API 재시도 {}/{}",
                                retrySignal.totalRetries() + 1, maxRetryAttempts)))
                .publishOn(Schedulers.boundedElastic())
                .flatMap(body -> Mono.fromRunnable(() -> handleResponseSync(productType, body)))
                .then();
    }

    /**
     * 금융감독원 API URI 생성
     */
    private String buildUri(ProductType productType) {
        return BASE_URL + productType.getApiPath() +
                "?auth=" + BANK_API_KEY +
                "&topFinGrpNo=020000&pageNo=1";
    }

    /**
     * API JSON 응답을 동기 처리하며 DB 저장 (트랜잭션 적용)
     */
    @Transactional
    public void handleResponseSync(ProductType productType, String body) {
        JSONObject result = new JSONObject(body).getJSONObject("result");
        JSONArray baseList = result.getJSONArray("baseList");
        JSONArray optionList = result.getJSONArray("optionList");

        Map<String, List<JSONObject>> productOptionsMap = new HashMap<>();
        IntStream.range(0, optionList.length())
                .mapToObj(optionList::getJSONObject)
                .forEach(optionJson -> {
                    String productCode = optionJson.getString("fin_prdt_cd");
                    productOptionsMap
                            .computeIfAbsent(productCode, k -> new ArrayList<>()) // 해당 상품 코드가 없으면 새 리스트 생성
                            .add(optionJson); // 옵션 추가
                });

        IntStream.range(0, baseList.length())
                .forEach(i -> processProduct(productType, baseList.getJSONObject(i), productOptionsMap));
    }

    /**
     * 개별 상품 처리: 은행 조회 혹은 저장 후 상품 저장
     */
    private void processProduct(ProductType type, JSONObject prod, Map<String, List<JSONObject>> productOptionsMap) {
        String bankCode = prod.getString("fin_co_no");
        Bank bank = createOrFindBank(bankCode, prod.getString("kor_co_nm"));

        switch (type) {
            case DEPOSIT -> saveDeposit(prod, bank, productOptionsMap); // Map 전달
            case SAVING -> saveSaving(prod, bank, productOptionsMap); // Map 전달 (나중에 구현)
        }
    }


    /**
     * 은행 조회 또는 신규 저장
     */
    private Bank createOrFindBank(String code, String name) {
        return bankRepository.findByCode(code)
                .orElseGet(() -> {
                    Bank newBank = new Bank();
                    newBank.setCode(code);
                    newBank.setName(name);
                    return bankRepository.save(newBank);
                });
    }

    /**
     * 예금상품 저장 (중복 이름 + 은행인 경우 저장 안함)
     */
    private void saveDeposit(JSONObject p, Bank bank, Map<String, List<JSONObject>> productOptionsMap) {
        if (depositProductRepository.findByNameAndBank(p.getString("fin_prdt_nm"), bank).isPresent()) return;

        DepositProduct dep = DepositProduct.createProduct(
                bank, p.getString("fin_prdt_cd"), p.getString("fin_prdt_nm"),
                p.optString("join_way", null), p.optString("mtrt_int", null),
                p.optString("spcl_cnd", null), p.optString("join_deny", null),
                p.optString("join_member", null), p.optString("etc_note", null),
                p.optLong("max_limit", 0),
                parseYearMonth(p.optString("dcls_month", null)),
                parseDate(p.optString("dcls_strt_day", null)),
                parseDate(p.optString("dcls_end_day", null)),
                parseDateTime(p.optString("fin_co_subm_day", null))
        );

        // Map에서 해당 상품의 옵션만 가져와 순회
        String currentProductCode = p.getString("fin_prdt_cd");
        List<JSONObject> optionsForCurrentProduct = productOptionsMap.getOrDefault(currentProductCode, Collections.emptyList());

        for (JSONObject o : optionsForCurrentProduct) { // 해당 상품의 옵션만 순회
            dep.addOption(DepositProductOption.createDepositProductOption(
                    dep, o.optString("intr_rate_type", null), o.optString("intr_rate_type_nm", null),
                    o.optInt("save_trm", 0), o.optDouble("intr_rate", 0.0), o.optDouble("intr_rate2", 0.0)));
        }

        depositProductRepository.save(dep);
    }

    /**
     * 적금상품 저장
     */
    private void saveSaving(JSONObject p, Bank bank, Map<String, List<JSONObject>> productOptionsMap) {
        // TODO: 기존 적금 저장 로직 구현
    }

    /* 날짜 파싱 유틸리티 */
    private LocalDate parseYearMonth(String s) {
        return Optional.ofNullable(s)
                .filter(t -> !t.isEmpty())
                .map(t -> LocalDate.parse(t + "01", DAY_FMT))
                .orElse(null);
    }

    private LocalDate parseDate(String s) {
        return Optional.ofNullable(s)
                .filter(t -> !t.isEmpty())
                .map(t -> LocalDate.parse(t, DAY_FMT))
                .orElse(null);
    }

    private LocalDateTime parseDateTime(String s) {
        return Optional.ofNullable(s)
                .filter(t -> !t.isEmpty())
                .map(t -> LocalDateTime.parse(t, TIME_FMT))
                .orElse(null);
    }
}
