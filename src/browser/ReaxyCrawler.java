package browser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriver.Navigation;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

/**
 * @author kulen
 * @createTime Sep 13, 2014 2:56:02 PM
 * @desc
 */
public class ReaxyCrawler {

	static final Logger log = LogManager.getLogger(ReaxyCrawler.class);

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

	public void loginSite(String user, String password) {
		spiderData = new LinkedHashMap<>();
		log.info("开始登录网站, user:{}  password:{}", user, password);
		navigation.to("https://www-reaxys-com.ezproxy.proxy.library.oregonstate.edu");
		window.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);

		WebElement nameEle = window.findElement(By.id("user"));
		WebElement passEle = window.findElement(By.id("pass"));
		nameEle.sendKeys(user);
		passEle.sendKeys(password);
		WebElement loginEle = window.findElement(By.xpath("//input[@type='submit']"));
		loginEle.click();
		String currentUrl = window.getCurrentUrl();
		log.info("网站登录完成, URL:{}", currentUrl);
		// 登录成功URL地址
		// https://www-reaxys-com.ezproxy.proxy.library.oregonstate.edu/reaxys/secured/search.do;jsessionid=C15C99A5092ECFA55D2D0BA21D72D454
		// https://www-reaxys-com.ezproxy.proxy.library.oregonstate.edu/reaxys/secured/search.do;jsessionid=333DB0B6F0780E0235B1DFAAB1A0447D
		// 登录失败URL地址
		// https://login.ezproxy.proxy.library.oregonstate.edu/login
		// https://login.ezproxy.proxy.library.oregonstate.edu/login
	}

	public void queryCas(String cas) throws Exception {
		try {
			log.info("开始查询Cas:{} 数据, 当前URL:{}", cas, window.getCurrentUrl());
			WebElement textEle = window.findElement(By.className("quickSearchField"));
			textEle.sendKeys(cas);
			WebElement goEle = window.findElement(By.xpath("//input[@value='Go']"));
			goEle.click();

			// 等待showDetail, 加载产品的物化数据
			log.info("查找元素之前URL:{}", window.getCurrentUrl());
			List<WebElement> showDetails = window.findElements(By.linkText("Show Details"));
			log.info("产品搜索结果页URL:{}", window.getCurrentUrl());
			// showDetail.click();

			// 跳转到产品详情页
			// log.info("v1:{}",
			// window.getPageSource().indexOf("class=\"label\""));
			List<WebElement> eles = window.findElements(By.className("label"));
			for (WebElement ele : eles) {
				ele.click();
				break;
			}
			log.info("找到元素的数量大小为:{}", eles.size());
			WebElement query = window.findElement(By.id("modifiedMenuItem_2_text"));
			log.info("detail:{}", query.toString());
			query.click();
			// 验证mol文件页是否已经打开
			window.findElement(By.className("quickSearchField"));
			log.info("产品详情页URL:{}", window.getCurrentUrl());
			this.writeFileAsString(cas + "_detail.html", window.getPageSource());
			this.parseDetailPageContent(window.getPageSource());
			navigation.back();

			// 打开产品的物化数据页面
			showDetails = window.findElements(By.linkText("Show Details"));
			showDetails.get(0).click();
			Thread.sleep(10 * 1000l);
			this.writeFileAsString(cas + "_physical.html", window.getPageSource());
			this.parseMainPageConent(window.getPageSource());

			// 打开合成路线页面
			WebElement synthesize = window.findElement(By.linkText("Synthesize"));
			synthesize.click();
			WebElement manualSyn = window.findElement(By.id("modifiedMenuItem_0_text"));
			manualSyn.click();
			log.info("合成路线页URL:{}", window.getCurrentUrl());
			// 验证合成路线页是否已经加载完成
			window.findElement(By.id("content"));
			Thread.sleep(18 * 1000l);
			this.writeFileAsString(cas + "_synthesis.html", window.getPageSource());
			this.parseSynthesisPageContent(window.getPageSource());
			log.info("CAS号:{} 数据查询完成", cas);
			log.info("结果:{}", om.writeValueAsString(spiderData));

			// 清空搜索内容
			// WebElement textEleClr =
			// window.findElement(By.className("quickSearchField"));
			// textEleClr.clear();
		} catch (Exception e) {
			System.out.println(">>>>:" + e.getMessage());
			e.printStackTrace();
		}
	}

	public void parseDetailPageFile(String filePath) throws Exception {
		String content = this.readFileAsString(filePath);
		this.parseDetailPageContent(content);
	}

	public void parseDetailPageContent(String content) throws Exception {
		log.info("开始提取mol数据");
		Document doc = Jsoup.parse(content);
		Elements mols = doc.select("input[name=structure.molecule]");
		if (mols.size() == 1) {
			Element mol = mols.get(0);
			spiderData.put("mol", mol.attr("value"));
		}
	}

	public void parseMainPageFile(String filePath) throws Exception {
		String content = this.readFileAsString(filePath);
		this.parseMainPageConent(content);
	}

	public void parseMainPageConent(String content) throws Exception {
		log.info("开始提取主页面物化数据...");
		Document doc = Jsoup.parse(content);
		Elements dataEles = doc.select("table[id=main_table]>tbody>tr>td");
		Element valueEle = dataEles.get(2);

		// 提取名称, CAS号等数据
		Elements names = valueEle.select("b");
		for (Element name : names) {
			name.remove();
		}
		String valueText = valueEle.text();
		String[] vs = valueText.split(" : ");
		if (vs.length != names.size()) {
			log.info("数据解析出错!");
			throw new IllegalArgumentException("数据解析出错!");
		}
		for (int i = 0; i < names.size(); i++) {
			String name = names.get(i).text();
			String value = vs[i];
			if (value.startsWith(": ")) {
				value = value.substring(2, value.length());
			}
			// System.out.printf("name:%s value:%s\n", name, value);
			spiderData.put(name, value);
		}

		Elements mainEles = doc.select("div[class=reactions_subdetails_main_title_up]");
		for (Element mainEle : mainEles) {
			log.info("top level data>{}", mainEle.text());
			Elements eles = mainEle.nextElementSibling().select("div[class=reactions_subdetails_title_up]");
			// one level data map
			Map<String, Object> oldm = new LinkedHashMap<>();
			for (Element ele : eles) {
				log.info("second level data>>{}", ele.text());
				Elements _eleTemp = ele.nextElementSibling().select("table[class=reactions_subdetails]");
				if (_eleTemp.size() == 0) {
					continue;
				}
				Element tableEle = _eleTemp.get(0);
				// store value names
				List<String> svns = new ArrayList<>();
				Elements trs = tableEle.select("tr");
				List<Map<String, String>> dataList = new ArrayList<>();
				for (int i = 0; i < trs.size(); i++) {
					Element tr = trs.get(i);
					if (i == 0) {
						// table header names
						Elements thns = tr.select("th");
						for (Element thn : thns) {
							svns.add(thn.text());
						}
						continue;
					}

					// table td datas
					Elements ttds = tr.select("td");
					if (svns.size() != ttds.size()) {
						if (ttds.toString().contains("colspan")) {
							continue;
						}
						log.warn("数据不正确!!!, svns.size:{} ttds.size:{}  svns.values:{}", svns.size(), ttds.size(), svns);
						// log.info(ttds.toString());
						continue;
					}
					Map<String, String> dv = new LinkedHashMap<String, String>();
					for (int j = 0; j < ttds.size(); j++) {
						Element v = ttds.get(j);
						this.deleteInvalidLink(v);
						// format string
						String fs = "";
						// 首列数据直接取值， 其余列保留格式
						if (j == 0) {
							fs = v.text();
						} else {
							fs = v.toString();
							if (fs.startsWith("<td") && fs.endsWith("</td>")) {
								fs = fs.substring(fs.indexOf(">") + 1, fs.lastIndexOf("<"));
							}
						}
						fs = this.deleteInvalidString(fs);
						dv.put(svns.get(j), fs.trim());
					}
					dataList.add(dv);
				}
				oldm.put(ele.text(), dataList);
			}
			spiderData.put(mainEle.text(), oldm);
		}
	}

	public void parseSynthesisPageFile(String filePath) throws Exception {
		String content = this.readFileAsString(filePath);
		this.parseSynthesisPageContent(content);
	}

	public void parseSynthesisPageContent(String content) throws Exception {
		log.info("解析合成路线数据...");
		List<Map<String, Object>> synList = new ArrayList<>();
		Document doc = Jsoup.parse(content);
		Elements trs = doc.select("table[id=main_table]>tbody>tr");
		List<Map<String, Object>> synRouteList = null;
		List<Map<String, Object>> synConList = null;

		for (Element tr : trs) {
			// 合成概览图
			if (tr.attr("id").startsWith("rx_") && tr.attr("name").startsWith("scrolled")) {
				Elements forms = tr.select("form");
				if (synRouteList != null && synConList != null) {
					Map<String, Object> dataMap = new LinkedHashMap<String, Object>();
					// 合成路径 合成条件
					dataMap.put("syn_route", synRouteList);
					dataMap.put("syn_con_list", synConList);
					synList.add(dataMap);
				}
				synRouteList = new ArrayList<>();
				synConList = new ArrayList<>();
				for (int i = 0; i < forms.size(); i++) {
					Element form = forms.get(i);
					Map<String, Object> compoundMap = new LinkedHashMap<String, Object>();
					compoundMap.put("rx_rn", form.select("input[name=currentItem.rbrn]").get(0).attr("value"));
					compoundMap.put("rx_syn_id", form.select("input[name=currentItem.copy]").get(0).attr("value"));
					Elements checkEle = form.select("div[class=availabilitylinks]");
					String type = checkEle.size() > 0 ? "source" : "target";
					compoundMap.put("type", type);
					synRouteList.add(compoundMap);
				}
			}
			// 合成条件
			if (tr.attr("id").startsWith("tr_RXD_") && tr.attr("class").startsWith("showrxdfirst")) {
				Elements tds = tr.select("tr[class^=showrxdfirst]>td");
				Map<String, Object> conMap = new LinkedHashMap<String, Object>();
				for (int i = 0; i < tds.size(); i++) {
					if (i == 0) {
						continue;
					}
					Element td = tds.get(i);
					if (i == 1) {
						String v = td.text();
						conMap.put("yield", v);
					}
					if (i == 2) {
						this.deleteInvalidLink(td);
						String v = this.deleteTd(td);
						v = this.deleteInvalidString(v);
						conMap.put("conditions", v);
					}
					if (i == 3) {
						this.deleteInvalidLink(td);
						String v = this.deleteTd(td);
						v = this.deleteInvalidString(v);
						conMap.put("references", v);
					}
				}
				synConList.add(conMap);
			}

		}
		if (synRouteList != null && synConList != null) {
			Map<String, Object> dataMap = new LinkedHashMap<String, Object>();
			// 合成路径 合成条件
			dataMap.put("syn_route", synRouteList);
			dataMap.put("syn_con_list", synConList);
			synList.add(dataMap);
		}
		spiderData.put("synList", synList);
	}

	public void deleteInvalidLink(Element ele) {
		Elements as = ele.select("a");
		for (Element a : as) {
			if (a.attr("class").equalsIgnoreCase("notPrinted")) {
				a.remove();
			}
		}
	}

	public String deleteInvalidString(String v) {
		return v.replaceAll("[<br />]*\\s*[&nbsp;]{30,120}\\s*[&nbsp;]{30,120}\\s*<br />\\s*[&nbsp;]{6,30}", "");
	}

	public String deleteTd(Element v) {
		String fs = v.toString();
		if (fs.startsWith("<td") && fs.endsWith("</td>")) {
			fs = fs.substring(fs.indexOf(">") + 1, fs.lastIndexOf("<"));
		}
		return fs;
	}

	public String readFileAsString(String filePath) throws Exception {
		FileReader fr = new FileReader(filePath);
		BufferedReader br = new BufferedReader(fr);
		StringBuffer sb = new StringBuffer();
		while (true) {
			String s = br.readLine();
			if (s == null) {
				break;
			}
			sb.append(s);
			sb.append("\n");
		}
		br.close();
		fr.close();
		log.debug("Page Content:{}", sb.toString());
		return sb.toString();
	}

	public void writeFileAsString(String name, String content) throws Exception {
		FileWriter fr = new FileWriter("/home/kulen/MyProject/browser-crawler/html_file/" + name);
		BufferedWriter br = new BufferedWriter(fr);
		br.write(content);
		br.close();
		fr.close();
	}

	public static void main2(String args[]) throws Exception {
		ReaxyCrawler rc = new ReaxyCrawler();
		rc.parseMainPageFile("/home/kulen/MyProject/browser-crawler/html_file/13110-37-7_physical.html");
		rc.parseSynthesisPageFile("/home/kulen/MyProject/browser-crawler/html_file/13110-37-7_synthesis.html");
	}

	public static void main(String args[]) throws Exception {
		ReaxyCrawler rc = new ReaxyCrawler();
		rc.openBrowser();
		rc.loginSite("mastk", "Mustang2004");
		String cas = "1333-86-4"; // 没有任何搜索结果
		cas = "2052-07-5"; // 有多条正常结果
		cas = "2104-09-8"; // 有正常的搜索结果
		cas = "9004-65-3"; // 有结果，但是无详情页数据
		cas = "12316-37-7"; // 没有任何搜索结果
		cas = "128228-96-6"; // 结果正常
		rc.queryCas(cas);
	}
}
