package me.vinceh121.danboorutracker;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.FormattedMessage;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;
import io.vertx.ext.web.client.WebClient;
import me.vinceh121.danboorutracker.Config.Task;

public class DanbooruTracker {
	private static final Logger LOG = LogManager.getLogger(DanbooruTracker.class);
	private static final Pattern PAT_R34_LABEL = Pattern.compile("\\(([0-9]+)\\)");
	private final ObjectMapper mapper = new ObjectMapper();
	private final Config config;
	private final Vertx vertx;
	private final NetClient netClient;
	private final WebClient webClient;
	private NetSocket carbonSocket;

	public static void main(String[] args) throws StreamReadException, DatabindException, IOException {
		DanbooruTracker tracker = new DanbooruTracker();
		tracker.start();
	}

	public DanbooruTracker() throws StreamReadException, DatabindException, IOException {
		this.config = this.mapper.readValue(new File("/etc/danboorutracker/config.json"), Config.class);
		this.vertx = Vertx.vertx();
		this.netClient = this.vertx.createNetClient();
		this.webClient = WebClient.create(vertx);
	}

	public void start() {
		this.netClient.connect(this.config.getCarbonPort(), this.config.getCarbonHost())
				.onSuccess(s -> this.carbonSocket = s);

		this.vertx.setPeriodic(this.config.getCheckDelay(), this::doTask);
	}

	private void doTask(long id) {
		for (Task t : this.config.getTasks()) {
			this.webClient.getAbs(t.getUrl()).send().onSuccess(res -> {
				String count = String.valueOf(this.fetchCount(t, res.body()));
				this.submitMetric(t.getMetric(), count).onFailure(excep -> {
					LOG.error(new FormattedMessage("Metric submit failed for task {}", t.getUrl(), excep));
				});
			}).onFailure(excep -> LOG.error(new FormattedMessage("Request failed for task {}", t.getUrl()), excep));
		}
	}

	private int fetchCount(Task t, Buffer buf) {
		switch (t.getFetchMode()) {
		case ARRAY_SIZE:
			return new JsonArray(buf).size();
		case DANBOORU_AUTOCOMPLETE:
			return new JsonArray(buf).getJsonObject(0).getInteger("post_count");
		case R34_AUTOCOMPLETE:
			String label = new JsonArray(buf).getJsonObject(0).getString("label");
			Matcher matcher = PAT_R34_LABEL.matcher(label);
			matcher.find();
			return Integer.parseInt(matcher.group(1));
		default:
			throw new IllegalArgumentException("Unknown fetch mode" + t.getFetchMode());
		}
	}

	private Future<Void> submitMetric(String name, String value) {
		StringBuilder line = new StringBuilder();
		line.append("danboorutracker.");
		line.append(name);
		line.append(" ");
		line.append(value);
		line.append(" ");
		line.append(Long.toString(new Date().getTime() / 1000));
		line.append("\n");

		return this.carbonSocket.write(line.toString());
	}

	public NetSocket getCarbonSocket() {
		return carbonSocket;
	}
}
