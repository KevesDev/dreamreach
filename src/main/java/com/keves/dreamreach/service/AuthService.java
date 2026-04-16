package com.keves.dreamreach.service;

import com.keves.dreamreach.dto.DailyReward;
import com.keves.dreamreach.dto.LoginRequest;
import com.keves.dreamreach.dto.LoginResponse;
import com.keves.dreamreach.dto.RegisterRequest;
import com.keves.dreamreach.entity.*;
import com.keves.dreamreach.exception.DuplicateResourceException;
import com.keves.dreamreach.exception.ResourceNotFoundException;
import com.keves.dreamreach.repository.PlayerAccountRepository;
import com.keves.dreamreach.repository.PlayerProfileRepository;
import com.keves.dreamreach.repository.VerificationTokenRepository;
import com.keves.dreamreach.util.DisplayNameGenerator;
import com.keves.dreamreach.util.JwtService;
import com.keves.dreamreach.util.VerificationCodeGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * This service will coordinate the "Check -> Scramble -> Save" workflow.
 */
@Service
public class AuthService {

    private final PlayerAccountRepository accountRepository;
    private final PlayerProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final DisplayNameGenerator nameGenerator;
    private final VerificationTokenRepository tokenRepository;
    private final VerificationCodeGenerator codeGenerator;
    private final JwtService jwtService;
    private final RewardService rewardService;



    // Constructor Injection: Spring pulls the Repository and the BCrypt Bean from its toolbox
    public AuthService(PlayerAccountRepository playerAccountRepository,
                       PlayerProfileRepository playerProfileRepository,
                       PasswordEncoder passwordEncoder,
                       DisplayNameGenerator nameGenerator,
                       VerificationTokenRepository tokenRepository,
                       VerificationCodeGenerator codeGenerator,
                       JwtService jwtService,
                       RewardService rewardService) {
        this.accountRepository = playerAccountRepository;
        this.profileRepository = playerProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.nameGenerator = nameGenerator;
        this.tokenRepository = tokenRepository;
        this.codeGenerator = codeGenerator;
        this.jwtService = jwtService;
        this.rewardService = rewardService;
    }

    @Transactional // Ensures database integrity—if any step fails, the entire transaction rolls back
    public void register(RegisterRequest request) {
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("An account with this email already exists.");
        }

        String uniqueName = getGuaranteedUniqueName();

        PlayerAccount newAccount = new PlayerAccount();
        newAccount.setEmail(request.getEmail());
        newAccount.setPassword(passwordEncoder.encode(request.getPassword()));
        newAccount.setEnabled(false);

        PlayerProfile profile = new PlayerProfile();
        profile.setDisplayName(uniqueName);

        profile.setAccount(newAccount);
        newAccount.setProfile(profile);

        // Provision the Stockpile (Starting Resources)
        PlayerResources resources = new PlayerResources();
        resources.setFood(150);
        resources.setWood(100);
        resources.setStone(50);
        resources.setGold(0);
        resources.setGems(0);
        resources.setProfile(profile);
        profile.setResources(resources);

        // Provision the Workforce (Starting Population)
        PlayerPopulation population = new PlayerPopulation();
        population.setHappiness(50); // Neutral starting mood
        population.setIdlePeasants(5); // 5 starting workers
        population.setHunters(0);
        population.setWoodcutters(0);
        population.setStoneworkers(0);
        population.setProfile(profile);
        profile.setPopulation(population);

        // Provision the Settlement (Starting Architecture)
        PlayerStructures structures = new PlayerStructures();
        structures.setHouses(1); // 1 starting house so the 5 peasants have capacity
        structures.setTowers(0);
        structures.setBakeries(0);
        structures.setProfile(profile);
        profile.setStructures(structures);

        // save the new account to the database. Since we are using Cascade
        // between the profile and account, the profile will also automatically be saved.
        accountRepository.save(newAccount);

        // generate the 6-digit code that will be sent to email
        String code = codeGenerator.generateCode();

        // build the token entity
        VerificationToken token = new VerificationToken();
        token.setCode(code);
        token.setExpiryDate(java.time.LocalDateTime.now().plusHours(24)); // expiration time
        token.setAccount(newAccount); // link token to new account

        // save the token to the database
        tokenRepository.save(token);

        // DEBUG: Since we don't have an email server hooked up yet,
        // print it to the console so we can read it and test the API
        System.out.println("========== [SECURITY] ==========");
        System.out.println("Generated verification code for " + request.getEmail() + ": " + code);
        System.out.println("================================");
    }

    /**
     * Loops until the injected generator provides a name not found in the DB.
     */
    private String getGuaranteedUniqueName() {
        String candidateName;
        do {
            candidateName = nameGenerator.generateRandomName();
        } while (profileRepository.existsByDisplayName(candidateName));

        return candidateName;
    }

    /**
     * Validate 6-digit emailed code and unlock the associated account.
     */
    @Transactional
    public void verifyEmail(String code) {
        // find the token. If it doesn't exist, throw an error.
        VerificationToken token  = tokenRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification code."));

        // Check if the token is expired.
        if (token.getExpiryDate().isBefore(java.time.LocalDateTime.now())) {
            throw new IllegalArgumentException("Verification code has expired. Please request a new code.");
        }

        // unlock the account
        PlayerAccount account = token.getAccount();
        account.setEnabled(true);
        accountRepository.save(account);

        // clean up - delete the token so it can't be used twice.
        tokenRepository.delete(token);
    }

    /**
     * Authenticates user credentials and issues a JSON Web Token upon success.
     * Enforces account verification status before granting access.
     * Calculates daily login streaks.
     */
    public LoginResponse login(LoginRequest request) {

        PlayerAccount account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with the provided email."));

        // uses passwordEncoder to compare raw passwords against the stored BCrypt hash
        if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials provided.");
        }

        if (!account.isEnabled()) {
            throw new IllegalStateException("Account requires email verification before login is permitted.");
        }

        /** --------------------------------------------------------
         *  CHECK LOGIN STREAK
         *  --------------------------------------------------------
         */
        boolean isFirstLoginToday = false;

        // Calculate the current 'server day' from UTC
        LocalDate serverToday = LocalDate.now(ZoneOffset.UTC);

        if (account.getLastLoginDate() == null) {
            // this is the first time this account has logged in
            account.setConsecutiveLogins(1);
            isFirstLoginToday = true;
        } else {
            // Convert the exact historical login second into a flat calendar day in UTC
            LocalDate lastLoginDay = LocalDate.ofInstant(account.getLastLoginDate(), ZoneOffset.UTC);

            // Calculate exactly how many calendar days have passed
            long daysBetween = ChronoUnit.DAYS.between(lastLoginDay, serverToday);

            if (daysBetween >= 1) {
                // User is on a streak
                account.setConsecutiveLogins(account.getConsecutiveLogins() + 1);
            } else {
                // User broke their streak, set to 1
                account.setConsecutiveLogins(1);
            }
        }

        // Stamp the account with the exact current second and commit to the database
        account.setLastLoginDate(Instant.now());

        // Generate the track AND populate the PendingReward holding space
        List<DailyReward> track = rewardService.getOrGenerateTrack(account);

        // NOW save the account so the pending rewards and streaks are committed
        accountRepository.save(account);

        // Issue token
        String token = jwtService.generateToken(account.getEmail());

        return new LoginResponse(token, "Bearer", isFirstLoginToday, account.getConsecutiveLogins(), track);
    }
}
