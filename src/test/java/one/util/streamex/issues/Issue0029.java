/*
 * Copyright 2015 Tagir Valeev
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package one.util.streamex.issues;

import static java.lang.Integer.parseInt;
import static org.junit.Assert.assertEquals;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.junit.Test;

import one.util.streamex.StreamEx;

/**
 * https://github.com/amaembo/streamex/issues/29
 * 
 * @author Tagir Valeev
 */
public class Issue0029 {
	private static final String _192_168_0_0_16 = "192.168.0.0/16";
	private static final String EXPECTED = "[T[" + _192_168_0_0_16 + "]]";

	static class T implements Comparable<T> {
		SubnetInfo info;

		public T(String cidr) {
			SubnetUtils tmp = new SubnetUtils(cidr);
			tmp.setInclusiveHostCount(true);
			this.info = tmp.getInfo();
		}

		public boolean isParent(T child) {
			return this.info.isInRange(child.info.getNetworkAddress()) && rPrefix() <= child.rPrefix();
		}

		private int rPrefix() {
			String cidrSignature = info.getCidrSignature();
			String iPrefix = cidrSignature.substring(cidrSignature.lastIndexOf('/') + 1);
			return Integer.parseInt(iPrefix);
		}

		@Override
		public String toString() {
			return "T[" + info.getCidrSignature() + "]";
		}

		@Override
		public int compareTo(T o) {
			String[] b1 = info.getNetworkAddress().split("\\.");
			String[] b2 = o.info.getNetworkAddress().split("\\.");
			int res;
			for (int i = 0; i < 4; i++) {
				res = parseInt(b1[0]) - parseInt(b2[0]);
				if (res != 0)
					return res;
			}
			return rPrefix() - o.rPrefix();
		}
	}

	static class C {
		public static final boolean isNested(T a, T b) {
			return a.isParent(b) || b.isParent(a);
		}

		public static final T merge(T a, T b) {
			return a.isParent(b) ? a : b;
		}
	}

	private Stream<T> getTestData() {
		// Stream of parent net 192.168.0.0/16 follow by the 50 first childs
		// 192.168.X.0/24
		return Stream
				.concat(Stream.of(_192_168_0_0_16),
						IntStream.range(0, 50).mapToObj(String::valueOf).map(i -> "192.168." + i + ".0/24"))
				.map(T::new);
	}

	@Test
	public void testCollapse() {
		List<T> result = StreamEx.of(getTestData()).sorted().collapse(C::isNested, C::merge).toList();
		assertEquals(EXPECTED, result.toString());
	}

	@Test
	public void testPlain() {
		List<T> tmp = getTestData().sorted().collect(Collectors.toList());
		Iterator<T> it = tmp.iterator();
		T curr, last;
		curr = last = null;
		while (it.hasNext()) {
			T oldLast = last;
			last = curr;
			curr = it.next();
			if (last != null && last.isParent(curr)) {
				it.remove();
				curr = last;
				last = oldLast;
			}
		}
		List<T> result = tmp.stream().collect(Collectors.toList());
		assertEquals(EXPECTED, result.toString());
	}
}
