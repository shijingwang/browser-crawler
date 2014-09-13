package browser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriver.Navigation;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

/**
 * @author kulen
 * @createTime Sep 13, 2014 2:56:02 PM
 * @desc
 */
public class ReaxyCrawler {

	static final Logger log = LogManager.getLogger(ReaxyCrawler.class);

	public void openLoginPage() {
		log.info("启动浏览器...");
		System.setProperty("webdriver.firefox.bin", "/usr/bin/firefox");
		WebDriver window = new FirefoxDriver();
		// System.setProperty("webdriver.chrome.bin", "/usr/bin/google-chrome");
		// WebDriver window = new ChromeDriver();
		Navigation navigation = window.navigate();
		navigation.to("https://www-reaxys-com.ezproxy.proxy.library.oregonstate.edu");

		WebElement nameEle = window.findElement(By.id("user"));
		WebElement passEle = window.findElement(By.id("pass"));
		nameEle.sendKeys("vanness");
		passEle.sendKeys("VanNes2197");
		WebElement loginEle = window.findElement(By.xpath("//input[@type='submit']"));
		loginEle.click();
		log.info("Current URL:{}", window.getCurrentUrl());

		// 输入搜索条件
		WebElement searchEle = window.findElement(By.className("quickSearchField"));
		searchEle.sendKeys("13110-37-7");
		WebElement goEle = window.findElement(By.xpath("//input[@value='Go']"));
		goEle.click();
		// System.out.println(loginEle);
		System.out.println("");
	}

	public static void main(String args[]) {
		ReaxyCrawler rc = new ReaxyCrawler();
		rc.openLoginPage();
		
	}
}
