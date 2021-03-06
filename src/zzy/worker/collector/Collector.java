package zzy.worker.collector;

import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.SwingWorker;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import zzy.dialog.MessageDialog;
import zzy.util.Showable;
import zzy.view.collector.CollectorWindow;
import zzy.view.processor.AutoScrollPane;
import zzy.worker.processor.Processor;

/**
 * A collector that collects URLs according to the given inputs
 * 
 * @author Zhaoyi
 */
public class Collector implements Showable {
	// TODO: advanced collector class name separated by comma
	private String url;
	private String clazz;
	private String attr;
	private boolean isStarEnabled;
	private int from;
	private int to;

	private Set<String> collected;
	private List<String> failed;
	private AutoScrollPane console;
	private CollectorWindow parent;

	/**
	 * Construct a collector
	 * 
	 * @param parent  - the parent window of the collector
	 * @param console - the console to print message to
	 */
	public Collector(CollectorWindow parent, AutoScrollPane console) {
		this.parent = parent;
		this.console = console;
		collected = new HashSet<String>();
		failed = new LinkedList<String>();
	}

	// ------------------------------------------------------------------------------------------//

	/**
	 * Set base URL
	 * 
	 * @param url - base URL
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Set class name
	 * 
	 * @param clazz - class name
	 */
	public void setClazz(String clazz) {
		this.clazz = clazz;
	}

	/**
	 * Set attribute name
	 * 
	 * @param attr - attribute name
	 */
	public void setAttr(String attr) {
		this.attr = attr;
	}

	/**
	 * Set place holder enabling
	 * 
	 * @param isStarEnabled - place holder enabling
	 */
	public void setStarEnabled(boolean isStarEnabled) {
		this.isStarEnabled = isStarEnabled;
	}

	/**
	 * Set starting index
	 * 
	 * @param from - starting index
	 */
	public void setFrom(int from) {
		this.from = from;
	}

	/**
	 * Set ending index
	 * 
	 * @param to - ending index
	 */
	public void setTo(int to) {
		this.to = to;
	}

	/**
	 * Return collceted URLs
	 * 
	 * @return collected URLs
	 */
	public Set<String> getCollected() {
		return collected;
	}

	/**
	 * Return failed URLs
	 * 
	 * @return failed URLs
	 */
	public List<String> getFailed() {
		return failed;
	}

	// ------------------------------------------------------------------------------------------//

	/**
	 * Get the value of a certain attribute in the certain class
	 * 
	 * @param doc - the html document
	 */
	private int getAttrValuesByClass(Document doc) {
		Elements elements = doc.getElementsByClass(clazz);
		if (elements.size() == 0) {
			new MessageDialog(parent, "Error", "Elements with this class name not found");
			return 0 ;
		}
		String temp = null;
		for (Element element : elements) {
			temp = element.attr(attr);
			if (temp.length() == 0) {
				new MessageDialog(parent, "Error", "Element found but does not have this attribute");
				return 0 ;
			}
			else
				collected.add(temp); // .attr() can be empty string
		}
		return elements.size();
	}

	/**
	 * Collect URLs on a single base URL
	 */
	private void collectSingle() {
		collectSingle(this.url);
	}

	/**
	 * Collect URLs on a given url
	 * 
	 * @param url - the given base URL
	 */
	private void collectSingle(String url) {
		new SwingWorker<Void, Void>() {
			int i = 0;
			@Override
			protected Void doInBackground() throws Exception {
				Document doc = null;
				try {
					doc = Jsoup.connect(url).get();
				} catch (Exception e) {
					failed.add(url);
					return null;
				}
				i = getAttrValuesByClass(doc);			
				return null;
			}

			@Override
			protected void done() {
				console.log(i + " URLs collected on " + url);
			}
		}.execute();
	}

	/**
	 * Collect URLs from base URL by replacing * with index
	 */
	private void collectAll() {
		if (isStarEnabled) {
			for (int i = from; i <= to; i++)
				collectSingle(url.replace("*", "" + i));
		} else {
			collectSingle();
		}
//		if (failed.size() > 0)
//			console.log("Cannot collect URLs on:");
//		for (String url : failed)
//			console.log("    " + url);
	}

	/**
	 * Collect URLs depending on the input
	 */
	public void collect() {
		try {
			parent.updateCollector();
			collectAll();
		} catch (MalformedURLException ex) {
			new MessageDialog(parent, "Error", ex.getMessage());
			return;
		}
	}

	/**
	 * Collect URLs from failed URLs
	 */
	public void collectF() {
		if (!hasRecords(failed, parent))
			return;
		LinkedList<String> temp = new LinkedList<String>(failed);
		failed.clear();
		for (String url : temp)
			collectSingle(url);
	}

	/**
	 * Submit collected URLs to the processor
	 * 
	 * @param p - the processor to sbumit records to
	 */
	public void submit(Processor p) {
		if (collected.size() == 0) {
			new MessageDialog(parent, "Error", "There are no collected records");
			return;
		}

		int i = 0;
		for (String c : collected)
			if (p.add(c))
				i++;
		console.log(String.format("%d/%d records submitted", i, collected.size()));
		collected.clear();
	}

	// collection, simplified by Showable
	/**
	 * Show collected records
	 */
	public void showC() {
		showByType(collected, "Collected", parent);
	}

	/**
	 * Show failed records
	 */
	public void showF() {
		showByType(failed, "Failed", parent);
	}

	/**
	 * Show both records
	 */
	public void showB() {
		showBoth(collected, failed, "Collected", "Failed", parent);
	}

	/**
	 * Clear records
	 */
	public void clearCollection() {
		clearRecords(collected, failed, "Collected", "Failed", parent, console);
	}
}
