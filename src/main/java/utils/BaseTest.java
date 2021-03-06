package utils;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import entities.Browser;
import entities.ChromeBrowser;
import entities.FirefoxBrowser;
import enums.BrowserType;
import io.github.bonigarcia.wdm.DriverManagerType;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import pages.IndexPage;

import java.net.MalformedURLException;
import java.net.URI;

public class BaseTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseTest.class.getSimpleName());
    public static ThreadLocal<WebDriver> driver = new ThreadLocal<>();

    public void setup(String browserName) {
        LOGGER.trace("Setting up {} driver", browserName);
        try {
            BrowserType browserType = BrowserType.valueOf(browserName.toUpperCase());
            switch (browserType) {
                case CHROME:
                    setupDriver(new ChromeBrowser());
                    break;
                case FIREFOX:
                    setupDriver(new FirefoxBrowser());
                    break;
            }
        } catch (IllegalArgumentException exception) {
            LOGGER.error("BROWSER NOT FOUND");
            throw new NotFoundException(String.format("Browser '%s' not found. Please add", browserName));
        }
    }

    public void setupDriver(Browser browser) {
        WebDriver webDriver;

        if (Boolean.parseBoolean(System.getProperty("selenoid.enabled"))) {
            webDriver = setupWebDriverWithSelenoid(browser);
        } else {
            webDriver = setupWebDriverWithWebDriverManager(browser);
        }

        driver.set(webDriver);
    }

    @AfterMethod
    public void tearDown() {
        getDriver().quit();
    }

    @AfterClass
    public void terminate() {
        //Remove the ThreadLocalMap element
        driver.remove();
    }

    public IndexPage openStartPage() {
        WebDriver driver = getDriver();
        WebDriverRunner.setWebDriver(driver);
        String url = System.getProperty("url.address");
        LOGGER.trace("Navigating to {}", url);
        Selenide.open(url);
        return LoadingPageFactory.get(IndexPage.class);
    }

    private WebDriver setupWebDriverWithWebDriverManager(Browser browser) {
        LOGGER.trace("Setting up driver with WebDriverManager");
        BrowserType browserType = browser.getType();
        DriverManagerType driverManagerType = DriverManagerType.valueOf(browserType.name());
        WebDriverManager.getInstance(driverManagerType).setup();
        return browser.getDriver();
    }

    private WebDriver setupWebDriverWithSelenoid(Browser browser) {
        LOGGER.trace("Setting up driver with Selenoid");
        WebDriver webDriver = null;
        String selenoidHubAddress = System.getProperty("selenoid.hub.address");
        DesiredCapabilities capabilities = browser.getCapabilities();
        try {
            webDriver = new RemoteWebDriver(URI.create(selenoidHubAddress + "/wd/hub").toURL(), capabilities);
        } catch (MalformedURLException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return webDriver;
    }

    private WebDriver getDriver() {
        //Get driver from ThreadLocalMap
        WebDriver webDriver = driver.get();
        if (webDriver == null || ((RemoteWebDriver) webDriver).getSessionId() == null) {
            setupDriver(new ChromeBrowser());
            webDriver = driver.get();
        }

        return webDriver;
    }
}