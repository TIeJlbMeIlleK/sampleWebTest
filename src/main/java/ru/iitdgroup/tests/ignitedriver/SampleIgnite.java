package ru.iitdgroup.tests.ignitedriver;

import java.util.Arrays;
import java.util.List;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

/**
 * Стартер игнайта для тестовых нужд.
 */
public class SampleIgnite {

	public static Ignite runLocalignite(boolean isClient) {
		final IgniteConfiguration config = createConfig();
		config.setClientMode(isClient);
		return Ignition.start(config);
	}

	public static IgniteConfiguration createConfig() {
		final String[] addressesConf = new String[]{"127.0.0.1"};
		final IgniteConfiguration configuration = new IgniteConfiguration();
		final TcpDiscoverySpi discoSpi = new TcpDiscoverySpi();
		final TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
		final List<String> addresses = Arrays.asList(addressesConf);
		ipFinder
		    .setAddresses(addresses)
		    .setShared(true);
		discoSpi.setIpFinder(ipFinder);
		configuration.setDiscoverySpi(discoSpi);
		return configuration;
	}

	public static void main(String[] args) {
		runLocalignite(false);
	}

}
