// V2
import java.io.File;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class SeleniumTest {
    private WebDriver webDriver;
    private Process httpServerProcess;

    @BeforeEach
    public void setUp() throws Exception {
        // Suppress Selenium warnings
        java.util.logging.Logger.getLogger("org.openqa.selenium").setLevel(java.util.logging.Level.OFF);

        // Find chromedriver
        String[] driverPaths = {
            "/usr/bin/chromedriver",
            "/usr/local/bin/chromedriver",
            "/snap/bin/chromedriver",
            System.getProperty("user.home") + "/.cache/selenium/chromedriver/linux64/chromedriver",
            "/opt/chromedriver/chromedriver"
        };
        for (String path : driverPaths) {
            File f = new File(path);
            if (f.exists() && f.canExecute()) {
                // System.out.println("Found chromedriver: " + path);
                System.setProperty("webdriver.chrome.driver", path);
                break;
            }
        }

        ChromeOptions options = new ChromeOptions();
        
        // Find and set Chrome binary
        String[] chromePaths = {
            "/usr/bin/chromium-browser",
            "/usr/bin/chromium", 
            "/usr/bin/google-chrome",
            "/snap/bin/chromium"
        };
        for (String path : chromePaths) {
            if (new File(path).exists()) {
                // System.out.println("Found Chrome binary: " + path);
                options.setBinary(path);
                break;
            }
        }

        options.addArguments(
            "--headless",
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--disable-features=VizDisplayCompositor",
            "--use-gl=swiftshader",
            "--allow-file-access-from-files",
            "--disable-web-security",
            "--user-data-dir=/tmp/chrome-test-" + System.currentTimeMillis()
        );

        webDriver = new ChromeDriver(options);

        // Start Python HTTP server
        File htmlFile = new File("src/main/StyledPage.html").getCanonicalFile();
        String htmlUrl = startHttpServer(htmlFile);
        
        // System.out.println("Navigating to: " + htmlUrl);
        webDriver.get(htmlUrl);

        new WebDriverWait(webDriver, Duration.ofSeconds(10))
            .until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

        // Fix headless rendering issues
        Thread.sleep(2000);
        ((JavascriptExecutor) webDriver).executeScript(
            "document.body.style.display='none';" +
            "document.body.offsetHeight;" + 
            "document.body.style.display='block';"
        );

        // System.out.println("Current URL: " + webDriver.getCurrentUrl());
        // String pageSource = webDriver.getPageSource();
        // String[] lines = pageSource.split("\n");
        // int maxLines = Math.min(100, lines.length);
        // System.out.println("Page source (first " + maxLines + " lines):");
        // for (int i = 0; i < maxLines; i++) {
        //     System.out.println(lines[i]);
        // }
    }

    private String startHttpServer(File htmlFile) throws Exception {
        int port = 8000 + (int) (Math.random() * 1000);
        String directory = htmlFile.getParent();
        String fileName = htmlFile.getName();

        // System.out.println("Starting HTTP server on port " + port + " in directory: " + directory);

        ProcessBuilder pb = new ProcessBuilder("python3", "-m", "http.server", String.valueOf(port));
        pb.directory(new File(directory));
        pb.redirectErrorStream(true);

        httpServerProcess = pb.start();
        Thread.sleep(2000);

        if (!httpServerProcess.isAlive()) {
            throw new RuntimeException("HTTP server failed to start");
        }

        String url = "http://localhost:" + port + "/" + fileName;

        // Test connectivity
        for (int i = 0; i < 10; i++) {
            try {
                java.net.URL testUrl = new java.net.URL(url);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) testUrl.openConnection();
                connection.setConnectTimeout(1000);
                connection.setReadTimeout(1000);
                connection.setRequestMethod("HEAD");
                int responseCode = connection.getResponseCode();
                connection.disconnect();

                if (responseCode == 200) {
                    // System.out.println("HTTP server ready: " + url);
                    return url;
                }
            } catch (Exception e) {
                if (i == 9) {
                    throw new RuntimeException("HTTP server not responding: " + e.getMessage());
                }
                Thread.sleep(500);
            }
        }

        throw new RuntimeException("HTTP server failed to respond");
    }

    @AfterEach
    public void tearDown() {
        if (httpServerProcess != null) {
            httpServerProcess.destroy();
            try {
                httpServerProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                httpServerProcess.destroyForcibly();
            }
        }
        if (webDriver != null) {
            webDriver.quit();
        }
    }

    @Test
    public void testIdSelector() {
        WebElement p2 = webDriver.findElement(By.id("p2"));
        assertEquals("rgba(255, 0, 0, 1)", p2.getCssValue("color"));
    }

    @Test
    public void testClassSelector() {
        List<WebElement> elements = webDriver.findElements(By.className("class2"));
        for(WebElement element: elements) {
            assertEquals("rgba(0, 0, 255, 1)", element.getCssValue("color"));
        }
    }

    @Test
    public void testElementSelector() {
        List<WebElement> elements = webDriver.findElements(By.cssSelector("h2"));
        for(WebElement element: elements) {
            assertEquals("rgba(0, 255, 255, 1)", element.getCssValue("color"));
        }
    }
}
