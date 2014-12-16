package browser;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.openqa.selenium.Alert;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriver.Navigation;
import org.openqa.selenium.chrome.ChromeDriver;

/**
 * @author kulen
 * @createTime Sep 13, 2014 2:56:02 PM
 * @desc
 */
public class WaimaoCrawler {

	static final Logger log = LogManager.getLogger(WaimaoCrawler.class);

	ObjectMapper om = new ObjectMapper();

	WebDriver window;

	Navigation navigation;

	Map<String, Object> spiderData;

	public static final String mainSearchUrl = "https://www-reaxys-com.ezproxy.proxy.library.oregonstate.edu/reaxys/secured/search.do";

	public void openBrowser() {
		log.info("启动浏览器...");
		System.setProperty("webdriver.chrome.bin", "/usr/bin/google-chrome");
		System.setProperty("webdriver.chrome.driver", "/home/kulen/browser_driver/chromedriver");
		window = new ChromeDriver();
		navigation = window.navigate();
	}

	public void openPage() {
		navigation.to("http://localhost:8080/HttpTest/data.html");
		// 有Alert窗口
		try {
			Alert a = window.switchTo().alert();
			a.accept();
		} catch (Exception e) {
			System.out.println("Alert处理出错");
		}
		navigation.to("http://localhost:8080/HttpTest/test.html");
	}

	public static void main(String args[]) throws Exception {
		WaimaoCrawler rc = new WaimaoCrawler();
		rc.openBrowser();
		rc.openPage();
	}
}
