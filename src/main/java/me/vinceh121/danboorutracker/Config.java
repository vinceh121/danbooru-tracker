package me.vinceh121.danboorutracker;

import java.util.ArrayList;
import java.util.List;

public class Config {
	private final List<Task> tasks = new ArrayList<>();
	private int carbonPort;
	private String carbonHost;
	private long checkDelay = 600000;

	public List<Task> getTasks() {
		return tasks;
	}

	public int getCarbonPort() {
		return carbonPort;
	}

	public void setCarbonPort(int carbonPort) {
		this.carbonPort = carbonPort;
	}

	public String getCarbonHost() {
		return carbonHost;
	}

	public void setCarbonHost(String carbonHost) {
		this.carbonHost = carbonHost;
	}

	public long getCheckDelay() {
		return checkDelay;
	}

	public void setCheckDelay(long checkDelay) {
		this.checkDelay = checkDelay;
	}

	public static class Task {
		private String url, metric;
		private FetchMode fetchMode = FetchMode.ARRAY_SIZE;

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getMetric() {
			return metric;
		}

		public void setMetric(String metric) {
			this.metric = metric;
		}

		public FetchMode getFetchMode() {
			return fetchMode;
		}

		public void setFetchMode(FetchMode fetchMode) {
			this.fetchMode = fetchMode;
		}
	}

	public static enum FetchMode {
		ARRAY_SIZE, DANBOORU_AUTOCOMPLETE, R34_AUTOCOMPLETE;
	}
}
