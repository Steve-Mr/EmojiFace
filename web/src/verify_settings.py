from playwright.sync_api import Page, expect, sync_playwright
import time
import re

def verify_settings(page: Page):
    page.goto("http://localhost:3000")

    # Wait for the heading "Settings" which is in the desktop sidebar
    heading = page.get_by_role("heading", name="Settings")
    heading.wait_for()

    # 1. Verify English Default
    expect(heading).to_be_visible()
    expect(page.get_by_text("Language")).to_be_visible()
    expect(page.get_by_text("Theme")).to_be_visible()

    # 2. Switch to Chinese
    page.get_by_role("button", name="中文").click()
    expect(page.get_by_role("heading", name="设置")).to_be_visible()
    expect(page.get_by_text("语言")).to_be_visible()
    expect(page.get_by_text("主题")).to_be_visible()

    # 3. Switch to Dark Mode
    # Click "Dark" or "暗色"
    page.get_by_role("button", name="暗色").click()

    # Check if html has class dark
    html = page.locator("html")
    expect(html).to_have_class(re.compile(r"dark"))

    # 4. Take screenshot
    time.sleep(0.5)
    page.screenshot(path="/home/jules/verification/settings_verification.png")

if __name__ == "__main__":
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        # Desktop viewport
        page = browser.new_page(viewport={"width": 1280, "height": 800})
        try:
            verify_settings(page)
        finally:
            browser.close()
