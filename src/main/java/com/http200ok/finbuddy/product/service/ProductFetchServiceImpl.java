package com.http200ok.finbuddy.product.service;

import com.http200ok.finbuddy.bank.domain.Bank;
import com.http200ok.finbuddy.bank.repository.BankRepository;
import com.http200ok.finbuddy.product.domain.DepositProduct;
import com.http200ok.finbuddy.product.domain.DepositProductOption;
import com.http200ok.finbuddy.product.domain.SavingProduct;
import com.http200ok.finbuddy.product.domain.SavingProductOption;
import com.http200ok.finbuddy.product.repository.DepositProductRepository;
import com.http200ok.finbuddy.product.repository.SavingProductRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class ProductFetchServiceImpl implements ProductFetchService {
    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final DepositProductRepository depositProductRepository;
    private final SavingProductRepository savingProductRepository;
    private final BankRepository bankRepository;
    private final RestTemplate restTemplate;

    private static final String BASE_URL = "http://finlife.fss.or.kr/finlifeapi/";
    @Value("${bank.api.key}")
    private String BANK_API_KEY;

    private static final DateTimeFormatter LOCAL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    @Autowired
    public ProductFetchServiceImpl(DepositProductRepository depositProductRepository, SavingProductRepository savingProductRepository, BankRepository bankRepository) {
        this.depositProductRepository = depositProductRepository;
        this.savingProductRepository = savingProductRepository;
        this.bankRepository = bankRepository;
        this.restTemplate = new RestTemplate();
    }

    @Override
    @Transactional
    public void fetchAndSaveProducts(String productType) {
        try {
            String apiUrl = BASE_URL + productType + "ProductsSearch.json?auth=" + BANK_API_KEY + "&topFinGrpNo=020000&pageNo=1";
            ResponseEntity<String> response = sendRequest(apiUrl);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("API 응답이 올바르지 않습니다: " + response.getStatusCode());
            }

            JSONObject jsonObject = new JSONObject(response.getBody());
            JSONObject resultObject = jsonObject.getJSONObject("result");
            JSONArray baseList = resultObject.getJSONArray("baseList");
            JSONArray optionList = resultObject.getJSONArray("optionList");

            for (int i = 0; i < baseList.length(); i++) {
                processProduct(productType, baseList.getJSONObject(i), optionList);
            }
        } catch (Exception e) {
            System.err.println("API 요청 중 예외 발생: " + e.getMessage());
        }
    }

    private ResponseEntity<String> sendRequest(String apiUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, String.class);

        if (response.getStatusCode() == HttpStatus.TEMPORARY_REDIRECT) {
            String redirectUrl = response.getHeaders().getLocation().toString();
            response = restTemplate.exchange(redirectUrl, HttpMethod.GET, entity, String.class);
        }

        return response;
    }

    @Transactional
    private void processProduct(String productType, JSONObject productData, JSONArray optionList) {
        String bankCode = productData.getString("fin_co_no");
        String productCode = productData.getString("fin_prdt_cd");
        String productName = productData.getString("fin_prdt_nm");

        Bank bank = bankRepository.findByCode(bankCode)
                .orElseGet(() -> {
                    Bank newBank = new Bank();
                    newBank.setCode(bankCode);
                    newBank.setName(productData.getString("kor_co_nm"));
                    return bankRepository.save(newBank);
                });

        if (productType.equals("deposit")) {
            if (depositProductRepository.findByNameAndBank(productName, bank).isEmpty()) {
                createAndSaveDepositProduct(productData, bank, optionList);
            }
        } else if (productType.equals("saving")) {
            if (savingProductRepository.findByNameAndBank(productName, bank).isEmpty()) {
                createAndSaveSavingProduct(productData, bank, optionList);
            }
        }
    }

    private void createAndSaveDepositProduct(JSONObject productData, Bank bank, JSONArray optionList) {
        DepositProduct deposit = DepositProduct.createProduct(
                bank,
                productData.getString("fin_prdt_cd"),
                productData.getString("fin_prdt_nm"),
                productData.optString("join_way", null),
                productData.optString("mtrt_int", null),
                productData.optString("spcl_cnd", null),
                productData.optString("join_deny", null),
                productData.optString("join_member", null),
                productData.optString("etc_note", null),
                productData.optLong("max_limit", 0),
                parseYearMonthToLocalDate(productData.optString("dcls_month", null)),
                parseLocalDate(productData.optString("dcls_strt_day", null)),
                parseLocalDate(productData.optString("dcls_end_day", null)),
                parseLocalDateTime(productData.optString("fin_co_subm_day", null))
        );
        depositProductRepository.save(deposit);

        for (int j = 0; j < optionList.length(); j++) {
            JSONObject optionObj = optionList.getJSONObject(j);
            if (optionObj.getString("fin_prdt_cd").equals(productData.getString("fin_prdt_cd"))) {
                DepositProductOption option = DepositProductOption.createDepositProductOption(
                        deposit,
                        optionObj.optString("intr_rate_type", null),
                        optionObj.optString("intr_rate_type_nm", null),
                        optionObj.optInt("save_trm", 0),
                        optionObj.optDouble("intr_rate", 0.0),
                        optionObj.optDouble("intr_rate2", 0.0)
                );
                deposit.addOption(option);
            }
        }
    }

    private void createAndSaveSavingProduct(JSONObject productData, Bank bank, JSONArray optionList) {
        SavingProduct saving = SavingProduct.createProduct(
                bank,
                productData.getString("fin_prdt_cd"),
                productData.getString("fin_prdt_nm"),
                productData.optString("join_way", null),
                productData.optString("mtrt_int", null),
                productData.optString("spcl_cnd", null),
                productData.optString("join_deny", null),
                productData.optString("join_member", null),
                productData.optString("etc_note", null),
                productData.optLong("max_limit", 0),
                parseYearMonthToLocalDate(productData.optString("dcls_month", null)),
                parseLocalDate(productData.optString("dcls_strt_day", null)),
                parseLocalDate(productData.optString("dcls_end_day", null)),
                parseLocalDateTime(productData.optString("fin_co_subm_day", null))
        );
        savingProductRepository.save(saving);

        for (int j = 0; j < optionList.length(); j++) {
            JSONObject optionObj = optionList.getJSONObject(j);
            if (optionObj.getString("fin_prdt_cd").equals(productData.getString("fin_prdt_cd"))) {
                SavingProductOption option = SavingProductOption.createSavingProductOption(
                        saving,
                        optionObj.optString("intr_rate_type", null),
                        optionObj.optString("intr_rate_type_nm", null),
                        optionObj.optInt("save_trm", 0),
                        optionObj.optDouble("intr_rate", 0.0),
                        optionObj.optDouble("intr_rate2", 0.0),
                        optionObj.optString("rsrv_type", null),
                        optionObj.optString("rsrv_type_nm", null)
                );
                saving.addOption(option);
            }
        }
    }

    // 날짜 변환 함수 추가
    private LocalDate parseYearMonthToLocalDate(String dateStr) {
        return Optional.ofNullable(dateStr)
                .filter(s -> !s.isEmpty())
                .map(s -> LocalDate.parse(s + "01", LOCAL_DATE_FORMATTER)) // YYYYMM → YYYYMM01 → LocalDate 변환
                .orElse(null);
    }

    private LocalDate parseLocalDate(String dateStr) {
        return Optional.ofNullable(dateStr)
                .filter(s -> !s.isEmpty())
                .map(s -> LocalDate.parse(s, LOCAL_DATE_FORMATTER))
                .orElse(null);
    }

    private LocalDateTime parseLocalDateTime(String dateStr) {
        return Optional.ofNullable(dateStr)
                .filter(s -> !s.isEmpty())
                .map(s -> LocalDateTime.parse(s, LOCAL_DATE_TIME_FORMATTER))
                .orElse(null);
    }
}
