package com.ecom.Ecommerce_SpringBoot.e2e;

import com.ecom.Ecommerce_SpringBoot.support.IntegrationTestBase;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Playwright + Spring Boot integration: starts the app on a random HTTP port,
 * launches Chromium once per class, and provides per-test {@link BrowserContext}
 * helpers so each test gets fresh cookies.
 *
 * <p>Browser visibility is controlled by system properties so the same test
 * runs in dev (headed, slowed down) and CI (headless, fast):
 * <pre>
 *   -Dbrowser.e2e=true            required — without this, the whole class is skipped
 *   -Dbrowser.headless=false      open a visible window (default: true)
 *   -Dbrowser.slowmo=400          ms delay between actions (default: 0)
 * </pre>
 *
 * <p>Inherits {@link IntegrationTestBase} so we get the H2 profile, the mail
 * mock, and the per-test DB reset for free.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfSystemProperty(named = "browser.e2e", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("e2e")
public abstract class BrowserTestBase extends IntegrationTestBase {

    @LocalServerPort protected int port;

    protected Playwright playwright;
    protected Browser browser;
    protected String baseUrl;

    @BeforeAll
    void launchBrowser() {
        boolean headless = Boolean.parseBoolean(System.getProperty("browser.headless", "true"));
        double slowMo = Double.parseDouble(System.getProperty("browser.slowmo", "0"));
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(headless)
                .setSlowMo(slowMo));
        baseUrl = "http://localhost:" + port;
    }

    @AfterAll
    void closeBrowser() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    /** Open a fresh, isolated browser context (think: a new incognito window). */
    protected BrowserContext newContext() {
        return browser.newContext();
    }

    /**
     * Save a full-page screenshot under {@code target/playwright-screenshots/}
     * for post-mortem debugging. Swallows IO errors — diagnostics shouldn't
     * mask the real test failure.
     */
    public static void screenshot(Page page, String name) {
        try {
            Path dir = Paths.get("target", "playwright-screenshots");
            Files.createDirectories(dir);
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(dir.resolve(name + ".png"))
                    .setFullPage(true));
            System.err.println("Saved screenshot to " + dir.resolve(name + ".png").toAbsolutePath());
        } catch (Exception e) {
            System.err.println("Failed to save screenshot: " + e);
        }
    }
}
