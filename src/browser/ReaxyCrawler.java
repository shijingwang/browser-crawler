package browser;

import java.io.BufferedReader;
import java.io.FileReader;
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
import org.openqa.selenium.firefox.FirefoxDriver;

/**
 * @author kulen
 * @createTime Sep 13, 2014 2:56:02 PM
 * @desc
 */
public class ReaxyCrawler {

	static final Logger log = LogManager.getLogger(ReaxyCrawler.class);

	static final ObjectMapper om = new ObjectMapper();

	public void openLoginPage() {
		log.info("启动浏览器...");
		System.setProperty("webdriver.firefox.bin", "/usr/bin/firefox");
		System.setProperty("webdriver.firefox.bin", "C:\\Program Files (x86)\\Mozilla Firefox\\firefox.exe");
		WebDriver window = new FirefoxDriver();
		// System.setProperty("webdriver.ie.bin", "C:\\Program Files\\Internet Explorer\\iexplore.exe");
		// WebDriver window = new InternetExplorerDriver();
		// System.setProperty("webdriver.chrome.bin", "/usr/bin/google-chrome");
		// System.setProperty("webdriver.chrome.bin",
		// "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe");
		// WebDriver window = new ChromeDriver();
		Navigation navigation = window.navigate();
		navigation.to("https://www-reaxys-com.ezproxy.proxy.library.oregonstate.edu");
		window.manage().timeouts().implicitlyWait(60, TimeUnit.SECONDS);

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
		// String s = window.getPageSource();
		// log.info("返回的内容为:{}", s);

		// 显示物化性质数据
		// WebElement showDetail =
		// window.findElement(By.linkText("Show Details"));
		// showDetail.click();

		// 打开合成路线数据
		// WebElement synthesize =
		// window.findElement(By.linkText("Synthesize"));
		// synthesize.click();
		// WebElement manualSyn =
		// window.findElement(By.id("modifiedMenuItem_0_text"));
		// manualSyn.click();

		WebElement compoundDetail = window.findElement(By.id("modifiedMenuItem_2_text"));
		compoundDetail.click();
		WebElement html = window.findElement(By.tagName("html"));
		log.info("value:{}", html.toString());
		log.info("Html:{}", window.getPageSource());
		log.info("程序搜索完成");
	}

	public void parsePage() throws Exception {
		FileReader fr = new FileReader("D:/1.html");
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

		Map<String, Object> dataMap = new LinkedHashMap<String, Object>();

		Document doc = Jsoup.parse(sb.toString());
		Elements dataEles = doc.select("table[id=main_table]>tbody>tr>td");
		Element valueEle = dataEles.get(2);

		Elements names = valueEle.select("b");

		for (Element name : names) {
			name.remove();
		}
		System.out.println("--------------------");
		// System.out.println(valueEle.toString());
		// System.out.println(valueEle.text());
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
			dataMap.put(name, value);
		}

		Elements mainEles = doc.select("div[class=reactions_subdetails_main_title_up]");
		// top level data map
		Map<String, Object> tldm = new LinkedHashMap<>();
		for (Element mainEle : mainEles) {
			System.out.println(">" + mainEle.text());
			Elements eles = mainEle.nextElementSibling().select("div[class=reactions_subdetails_title_up]");
			// one level data map
			Map<String, Object> oldm = new LinkedHashMap<>();
			for (Element ele : eles) {
				System.out.println(">>>>" + ele.text());
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
			tldm.put(mainEle.text(), oldm);
		}
		dataMap.put("detailInfo", tldm);
		// System.out.println(dataMap);
		System.out.println(om.writeValueAsString(dataMap));
	}

	public void testRegex() {
		String v = "<b>Adams et al.</b> <br />Journal of the American Chemical Society,&nbsp;<b>1926 </b>,&nbsp; vol.&nbsp;48,&nbsp;p.&nbsp;1768<br /> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;  <br /> &nbsp;";
		// System.out.println(v.replaceAll("<br />\\s*[&nbsp;]{30,120}\\s*<br />\\s*[&nbsp;]{6,30}",
		// ""));
		v = "<br /> &nbsp;dfa abcd<br /> &nbsp;$";
		v = "";
		System.out.println(v.replaceAll("<br /> &nbsp;$", ""));
	}

	public void parseSynthesis() throws Exception {
		FileReader fr = new FileReader("D:/2.html");
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

		List<Map<String, Object>> synList = new ArrayList<>();

		Document doc = Jsoup.parse(sb.toString());
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
		String resultJson = om.writeValueAsString(synList);
		log.info("合成路径的数据为:{}", resultJson);
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

	public static void main(String args[]) throws Exception {
		ReaxyCrawler rc = new ReaxyCrawler();
		rc.openLoginPage();
		// rc.parsePage();
		// rc.testRegex();
		// rc.parseSynthesis();
		String v = "<b>Matsunaga; Miyajima</b> <br />Molecular crystals and liquid crystals,&nbsp;<b>1984 </b>,&nbsp; vol.&nbsp;104,&nbsp; # 3-4 &nbsp;p.&nbsp;353 - 359<br /> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;  <br /> &nbsp; ";
		// System.out.println(rc.deleteInvalidString(v));
	}
}
