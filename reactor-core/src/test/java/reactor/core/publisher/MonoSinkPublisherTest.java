/*
 * Copyright (c) 2011-2018 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactor.core.publisher;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MonoSinkPublisherTest {

	@Test
	public void smokeTestSubscribeBeforeEmit() {
		List<String> list = new ArrayList<>();
		MonoSinkPublisher<String> sink = new MonoSinkPublisher<>();

		sink.subscribe(new BaseSubscriber<String>() {
			@Override
			protected void hookOnNext(String value) {
				list.add(value);
			}
		});

		sink.asMono()
		    .subscribe(list::add);

		sink.success("foo");

		assertThat(list).containsExactly("foo", "foo");
	}

	@Test
	public void smokeTestSubscribeAfterEmit() {
		List<String> list = new ArrayList<>();
		MonoSinkPublisher<String> sink = new MonoSinkPublisher<>();

		sink.success("foo");

		sink.subscribe(new BaseSubscriber<String>() {
			@Override
			protected void hookOnNext(String value) {
				list.add(value);
			}
		});

		sink.asMono()
		    .subscribe(list::add);

		assertThat(list).containsExactly("foo", "foo");
	}

	@Test
	public void smokeTestUseAsToggle() throws InterruptedException {
		MonoSinkPublisher<Boolean> sink = MonoProcessor.sink();
		List<Long> list = new ArrayList<>();

		Flux.interval(Duration.ofMillis(100))
		    .takeUntilOther(sink)
		    .subscribe(list::add);

		Thread.sleep(310);

		assertThat(list).containsExactly(0L, 1L, 2L);
	}

}