package com.zingzing.backend.service;

import com.zingzing.backend.entity.*;
import com.zingzing.backend.entity.enums.BrandPaymentAccessRole;
import com.zingzing.backend.entity.enums.CreatorPayoutSchedule;
import com.zingzing.backend.entity.enums.PayoutMethodType;
import com.zingzing.backend.entity.enums.UserRole;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentDomainIntegrationTest {

    @Autowired
    private CreatorRepository creatorRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private CreatorPayoutPreferenceRepository creatorPayoutPreferenceRepository;

    @Autowired
    private BrandPaymentAccessRepository brandPaymentAccessRepository;

    @Autowired
    private PayoutMethodService payoutMethodService;

    @Autowired
    private WithdrawalService withdrawalService;

    @Autowired
    private BrandPaymentsService brandPaymentsService;

    @Test
    void creatorPayoutMethodLifecycleAndWithdrawalThreshold_areEnforced() {
        Creator creator = saveCreator("creator_one", "creator.one@test.pk", "+923001110001");
        walletRepository.save(Wallet.builder().creator(creator).availableBalance(50000).pendingBalance(0).totalEarned(150000).build());
        creatorPayoutPreferenceRepository.save(CreatorPayoutPreference.builder()
                .creator(creator)
                .minimumPayoutAmount(10000)
                .payoutSchedule(CreatorPayoutSchedule.MANUAL)
                .build());

        PayoutMethod payoutMethod = payoutMethodService.create(
                creator.getId(),
                UserRole.CREATOR,
                PayoutMethodType.BANK_TRANSFER,
                "Meezan Main",
                "PK36MEZN0001200001234567",
                true,
                "Meezan Bank"
        );

        ApiException belowMinimum = assertThrows(ApiException.class, () ->
                withdrawalService.requestWithdrawal(creator.getId(), UserRole.CREATOR, payoutMethod.getId(), 5000));
        assertEquals(400, belowMinimum.getStatus().value());

        WithdrawalRequest accepted = withdrawalService.requestWithdrawal(
                creator.getId(),
                UserRole.CREATOR,
                payoutMethod.getId(),
                12000
        );

        assertNotNull(accepted.getId());
        Wallet updated = walletRepository.findByCreatorId(creator.getId()).orElseThrow();
        assertEquals(38000, updated.getAvailableBalance());
        assertEquals(12000, updated.getPendingBalance());
    }

    @Test
    void pakistanPayoutValidation_rejectsInvalidFormats() {
        Creator creator = saveCreator("creator_two", "creator.two@test.pk", "+923001110002");

        ApiException invalidIban = assertThrows(ApiException.class, () ->
                payoutMethodService.create(
                        creator.getId(),
                        UserRole.CREATOR,
                        PayoutMethodType.BANK_TRANSFER,
                        "Bad IBAN",
                        "INVALID-IBAN",
                        true,
                        "Meezan Bank"
                ));
        assertEquals(400, invalidIban.getStatus().value());

        ApiException invalidWallet = assertThrows(ApiException.class, () ->
                payoutMethodService.create(
                        creator.getId(),
                        UserRole.CREATOR,
                        PayoutMethodType.STCPAY,
                        "Wallet",
                        "abc",
                        true,
                        null
                ));
        assertEquals(400, invalidWallet.getStatus().value());

        PayoutMethod validWallet = payoutMethodService.create(
                creator.getId(),
                UserRole.CREATOR,
                PayoutMethodType.STCPAY,
                "JazzCash",
                "+923001234567",
                true,
                null
        );
        assertNotNull(validWallet.getId());
    }

    @Test
    void brandPayoutGovernance_rolesAreEnforcedForControls() {
        Brand ownerBrand = saveBrand("brand_owner", "brand.owner@test.pk", "+922100010001");
        User viewerUser = saveBrandUserOnly("brand_viewer", "brand.viewer@test.pk", "+922100010002");
        User financeUser = saveBrandUserOnly("brand_finance", "brand.finance@test.pk", "+922100010003");

        brandPaymentAccessRepository.save(BrandPaymentAccess.builder()
                .brand(ownerBrand)
                .user(viewerUser)
                .role(BrandPaymentAccessRole.VIEWER)
                .build());
        brandPaymentAccessRepository.save(BrandPaymentAccess.builder()
                .brand(ownerBrand)
                .user(financeUser)
                .role(BrandPaymentAccessRole.FINANCE)
                .build());

        BrandPaymentsService.BrandScope viewerScope =
                brandPaymentsService.resolveBrandScope(viewerUser.getId(), UserRole.BRAND, ownerBrand.getId());
        ApiException viewerForbidden = assertThrows(ApiException.class, () ->
                brandPaymentsService.updateControls(
                        viewerUser.getId(),
                        viewerScope,
                        new BrandPaymentsService.UpdateControlsRequest(true, 3, 250000)
                ));
        assertEquals(403, viewerForbidden.getStatus().value());

        BrandPaymentsService.BrandScope financeScope =
                brandPaymentsService.resolveBrandScope(financeUser.getId(), UserRole.BRAND, ownerBrand.getId());
        BrandPaymentsService.BrandPayoutControlsResponse updated = brandPaymentsService.updateControls(
                financeUser.getId(),
                financeScope,
                new BrandPaymentsService.UpdateControlsRequest(true, 3, 250000)
        );

        assertEquals(3, updated.autoReleaseAfterDays());
        assertEquals(250000, updated.lowBalanceAlertThreshold());
    }

    private Creator saveCreator(String username, String email, String phone) {
        Creator creator = new Creator();
        creator.setUsername(username);
        creator.setEmail(email);
        creator.setPhone(phone);
        creator.setName("Creator " + username);
        creator.setRole(UserRole.CREATOR);
        creator.setActive(true);
        return creatorRepository.save(creator);
    }

    private Brand saveBrand(String username, String email, String phone) {
        Brand brand = new Brand();
        brand.setUsername(username);
        brand.setEmail(email);
        brand.setPhone(phone);
        brand.setName("Brand " + username);
        brand.setRole(UserRole.BRAND);
        brand.setActive(true);
        return brandRepository.save(brand);
    }

    private User saveBrandUserOnly(String username, String email, String phone) {
        User user = User.builder()
                .username(username)
                .email(email)
                .phone(phone)
                .name("User " + username)
                .role(UserRole.BRAND)
                .active(true)
                .build();
        return userRepository.save(user);
    }
}

