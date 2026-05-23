package com.ecom.Ecommerce_SpringBoot.e2e.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;

/**
 * Shared base for Page Objects. Wraps the raw {@link Page} and exposes a few
 * navigation helpers tuned to this app — most notably {@link #open(String)}
 * which uses {@code DOMCONTENTLOADED} instead of the default {@code load}, so
 * we don't hang waiting for Bootstrap / Font Awesome CDNs.
 */
public abstract class AbstractPage {

    protected final Page page;
    protected final String baseUrl;

    protected AbstractPage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    /** Navigate to a path relative to {@link #baseUrl} without waiting for external assets. */
    protected void open(String path) {
        page.navigate(baseUrl + path,
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
    }

    /** Raw page handle — escape hatch for ad-hoc assertions in tests. */
    public Page raw() {
        return page;
    }
}
