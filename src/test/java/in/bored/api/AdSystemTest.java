package in.bored.api;

import in.bored.api.dto.*;
import in.bored.api.model.*;
import in.bored.api.repo.*;
import in.bored.api.service.AdService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AdSystemTest {

        @Autowired
        private AdService adService;

        @Autowired
        private AdRepository adRepository;

        @Autowired
        private AdTargetingRuleRepository ruleRepository;

        @Autowired
        private UserProfileRepository userProfileRepository;

        @Autowired
        private AdImpressionRepository impressionRepository;

        private Long userId;

        @BeforeEach
        void setup() {
                // Clear data
                impressionRepository.deleteAll();
                ruleRepository.deleteAll();
                adRepository.deleteAll();
                userProfileRepository.deleteAll();

                // Create a user
                UserProfile user = new UserProfile();
                user.setUid("test-uid");
                user.setFirebaseUid("test-fb-uid");
                user.setAge(25);
                user.setState("CA");
                user.setGender("Male");
                user.setSubscriptionType(SubscriptionType.FREE);
                user = userProfileRepository.save(user);
                userId = user.getId();
        }

        @Test
        void testServeAd_NoRules() {
                // Create an Ad with no rules
                AdRequest adRequest = new AdRequest(
                                "Ad A", "IMAGE", "http://img.com/a.jpg", null, "Text", "Click", "http://url.com", true,
                                10, 5);
                AdResponse createdAd = adService.createAd(adRequest);

                // Serve ad
                AdResponse servedAd = adService.serveAd(userId);
                assertNotNull(servedAd);
                assertEquals(createdAd.id(), servedAd.id());

                // Verify impression
                assertEquals(1, impressionRepository.count());
        }

        @Test
        void testServeAd_MatchingRule() {
                AdRequest adRequest = new AdRequest(
                                "Targeted Ad", "IMAGE", "http://img.com", null, "Text", "Click", "http://url.com", true,
                                1, 5);
                AdResponse createdAd = adService.createAd(adRequest);

                // Add matching rule (Age 20-30, State CA)
                AdTargetingRuleRequest ruleRequest = new AdTargetingRuleRequest(
                                20, 30, "CA", null, null);
                adService.addRule(createdAd.id(), ruleRequest);

                AdResponse servedAd = adService.serveAd(userId);
                assertNotNull(servedAd);
                assertEquals(createdAd.id(), servedAd.id());
        }

        @Test
        void testServeAd_NonMatchingRule() {
                AdRequest adRequest = new AdRequest(
                                "Targeted Ad", "IMAGE", "http://img.com", null, "Text", "Click", "http://url.com", true,
                                1, 5);
                AdResponse createdAd = adService.createAd(adRequest);

                // Add NON-matching rule (State NY)
                AdTargetingRuleRequest ruleRequest = new AdTargetingRuleRequest(
                                null, null, "NY", null, null);
                adService.addRule(createdAd.id(), ruleRequest);

                AdResponse servedAd = adService.serveAd(userId);
                assertNull(servedAd, "Should not serve ad targeted for NY to CA user");
        }

        @Test
        void testServeAd_GenderMismatch() {
                AdRequest adRequest = new AdRequest(
                                "Gender Ad", "IMAGE", "http://img.com", null, "Text", "Click", "http://url.com", true,
                                1, 5);
                AdResponse createdAd = adService.createAd(adRequest);

                // Target Female
                AdTargetingRuleRequest ruleRequest = new AdTargetingRuleRequest(
                                null, null, null, "Female", null);
                adService.addRule(createdAd.id(), ruleRequest);

                AdResponse servedAd = adService.serveAd(userId); // User is Male
                assertNull(servedAd);
        }

        @Test
        void testServeAd_MultipleRules_OneMatches() {
                AdRequest adRequest = new AdRequest(
                                "Multi Rule Ad", "IMAGE", "http://img.com", null, "Text", "Click", "http://url.com",
                                true, 1, 5);
                AdResponse createdAd = adService.createAd(adRequest);

                // Rule 1: Mismatch (NY)
                adService.addRule(createdAd.id(), new AdTargetingRuleRequest(null, null, "NY", null, null));
                // Rule 2: Match (Male)
                adService.addRule(createdAd.id(), new AdTargetingRuleRequest(null, null, null, "Male", null));

                // Logic is OR? Check AdService implementation.
                // Yes: if (matchesRule(user, rule)) return true;

                AdResponse servedAd = adService.serveAd(userId);
                assertNotNull(servedAd, "Should match because one rule (Male) matches");
        }

        @Test
        void testWeightedServing() {
                // Create 2 ads: Prio 1 and Prio 99
                // Prio 1 should show ~50x more often than Prio 99.

                // Ad A: Prio 1
                AdResponse adA = adService.createAd(new AdRequest(
                                "Ad A", "IMAGE", null, null, null, null, null, true, 1, 5));

                // Ad B: Prio 99
                AdResponse adB = adService.createAd(new AdRequest(
                                "Ad B", "IMAGE", null, null, null, null, null, true, 99, 5));
                int countA = 0;
                int countB = 0;
                int runs = 1000;

                for (int i = 0; i < runs; i++) {
                        // Need to clear impressions? No, serveAd doesn't filter by impression Cap yet.
                        AdResponse served = adService.serveAd(userId);
                        if (served.id().equals(adA.id()))
                                countA++;
                        else if (served.id().equals(adB.id()))
                                countB++;
                }

                System.out.println("Weighted Test: Ad A (Prio 1): " + countA + ", Ad B (Prio 99): " + countB);

                // Expect A to be dominant
                assertTrue(countA > countB, "Ad A (Prio 1) should be shown more than Ad B (Prio 99)");
                assertTrue(countA > (runs * 0.8), "Ad A should take the vast majority of traffic");
        }

        @Test
        void testSubscriptionLogic() {
                AdRequest adRequest = new AdRequest(
                                "Ad Male OR CA", "IMAGE", null, null, null, null, null, true, 10, 5);
                adService.createAd(adRequest);

                // FREE user (default setup)
                assertNotNull(adService.serveAd(userId), "FREE user should get ads");

                // PREMIUM user
                UserProfile premiumUser = new UserProfile();
                premiumUser.setUid("prem-uid");
                premiumUser.setFirebaseUid("prem-fb");
                premiumUser.setSubscriptionType(SubscriptionType.PREMIUM);
                premiumUser = userProfileRepository.save(premiumUser);

                assertNull(adService.serveAd(premiumUser.getId()), "PREMIUM user should NOT get ads");

                // PAID user
                UserProfile paidUser = new UserProfile();
                paidUser.setUid("paid-uid");
                paidUser.setFirebaseUid("paid-fb");
                paidUser.setSubscriptionType(SubscriptionType.PAID);
                paidUser = userProfileRepository.save(paidUser);

                assertNull(adService.serveAd(paidUser.getId()), "PAID user should NOT get ads");
        }
}
