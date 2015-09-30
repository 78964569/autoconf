package me.config;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import me.config.api.IChangeListener;
import me.config.api.IChangeableConfig;
import me.config.api.IConfigFactory;
import me.config.impl.LocalConfig;
import me.config.impl.MergedConfig;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * 本地配置工厂
 * Created by lirui on 2015-09-30 22:25.
 */
public class ConfigFactory implements IConfigFactory {
	private final ConcurrentMap<String, IChangeableConfig> m = Maps.newConcurrentMap();
	private final Path basePath;

	public ConfigFactory(Path basePath) {
		this.basePath = basePath;
	}

	@Override
	public IChangeableConfig getConfig(String name) {
		IChangeableConfig c = m.get(name);
		if (c == null) {
			synchronized (this) {
				c = m.get(name);
				if (c == null) {
					c = newConfig(name);
					IChangeableConfig real = m.putIfAbsent(name, c);
					if (real != null) {
						c = real;
					}
				}
			}
		}
		return c;
	}

	@Override
	public IChangeableConfig getConfig(String name, IChangeListener listener) {
		return getConfig(name, listener, true);
	}

	@Override
	public IChangeableConfig getConfig(String name, IChangeListener listener, boolean loadAfterRegister) {
		IChangeableConfig config = getConfig(name);
		config.addListener(listener, loadAfterRegister);
		return config;
	}

	@Override
	public boolean hasConfig(String name) {
		return false;
	}

	private IChangeableConfig newConfig(String name) {
		CharMatcher matcher = CharMatcher.anyOf(",; |");
		if (matcher.matchesAnyOf(name)) {
			List<String> names = Splitter.on(matcher).trimResults().omitEmptyStrings().splitToList(name);
			List<IChangeableConfig> list = Lists.newArrayList();
			for (String i : names) {
				list.add(new LocalConfig(i, basePath.resolve(i)));
			}
			return new MergedConfig(list);
		} else {
			return new LocalConfig(name, basePath.resolve(name));
		}
	}
}
