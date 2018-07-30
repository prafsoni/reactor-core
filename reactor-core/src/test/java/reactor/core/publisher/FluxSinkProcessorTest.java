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

/**
 * @author Simon Baslé
 */
public class FluxSinkProcessorTest {

	@Test
	public void subscribeAndSink() throws InterruptedException {
		final EmitterProcessor<String> processor = EmitterProcessor.create(3);
		final FluxSinkProcessor<String> fsp = new FluxSinkProcessor<>(processor);
		final List<String> list = new ArrayList<>();

		Flux<String> main = fsp.asFlux()
		                       .map(s -> "value:" + s)
		                       .doOnNext(list::add);

		Flux.interval(Duration.ZERO, Duration.ofMillis(10))
		    .take(10)
		    .map(String::valueOf)
		    .subscribe(fsp);


		Thread.sleep(20);
		fsp.next("Hello");
		Thread.sleep(10);
		fsp.next("World!");
		Thread.sleep(50);
		fsp.complete();

		main.blockLast(Duration.ofSeconds(2));

		assertThat(list).startsWith("value:0","value:1")
		                .contains("value:2", "value:Hello","value:3","value:World!")
		                .endsWith("value:4","value:5","value:6");
	}

}