package com.http200ok.finbuddy.product.service;

import com.http200ok.finbuddy.bank.domain.Bank;
import com.http200ok.finbuddy.bank.repository.BankRepository;
import com.http200ok.finbuddy.product.domain.DepositProduct;
import com.http200ok.finbuddy.product.domain.DepositProductOption;
import com.http200ok.finbuddy.product.repository.DepositProductRepository;
import com.http200ok.finbuddy.product.repository.SavingProductRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    @Override
    public Mono<Void> fetchAndSaveProducts(String productType) {

        String uri = buildUri(productType);

        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(String.class)
                .publishOn(Schedulers.boundedElastic())
                .flatMap(body -> Mono.fromRunnable(() -> handleResponseSync(productType, body)))
                .then();
    }

    /**
     * 금융감독원 API URI 생성
     */
    private String buildUri(String productType) {
        String path;
        if ("deposit".equals(productType)) {
            path = "depositProductsSearch.json";
        } else if ("saving".equals(productType)) {
            path = "savingProductsSearch.json";
        } else {
            throw new IllegalArgumentException("지원하지 않는 productType: " + productType);
        }

        return BASE_URL + path +
                "?auth=" + BANK_API_KEY +
                "&topFinGrpNo=020000&pageNo=1";
    }

    /**
     * API JSON 응답을 동기 처리하며 DB 저장 (트랜잭션 적용)
     */
    @Transactional
    public void handleResponseSync(String productType, String body) {
        JSONObject result = new JSONObject(body).getJSONObject("result");
        JSONArray baseList = result.getJSONArray("baseList");
        JSONArray optionList = result.getJSONArray("optionList");

        IntStream.range(0, baseList.length())
                .forEach(i -> processProduct(productType, baseList.getJSONObject(i), optionList));
    }

    /**
     * 개별 상품 처리: 은행 조회 혹은 저장 후 상품 저장
     */
    private void processProduct(String type, JSONObject prod, JSONArray optionList) {
        String bankCode = prod.getString("fin_co_no");
        Bank bank = createOrFindBank(bankCode, prod.getString("kor_co_nm"));

        if ("deposit".equals(type)) {
            saveDeposit(prod, bank, optionList);
        } else if ("saving".equals(type)) {
            saveSaving(prod, bank, optionList);
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
    private void saveDeposit(JSONObject p, Bank bank, JSONArray optionList) {
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

        for (int j = 0; j < optionList.length(); j++) {
            JSONObject o = optionList.getJSONObject(j);
            if (!o.getString("fin_prdt_cd").equals(p.getString("fin_prdt_cd"))) continue;
            dep.addOption(DepositProductOption.createDepositProductOption(
                    dep, o.optString("intr_rate_type", null), o.optString("intr_rate_type_nm", null),
                    o.optInt("save_trm", 0), o.optDouble("intr_rate", 0.0), o.optDouble("intr_rate2", 0.0)));
        }

        depositProductRepository.save(dep);
    }

    /**
     * 적금상품 저장
     */
    private void saveSaving(JSONObject p, Bank bank, JSONArray optionList) {
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
