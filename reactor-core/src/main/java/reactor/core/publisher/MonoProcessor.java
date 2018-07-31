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

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;

/**
 * A {@link MonoProcessor} is a simplified {@link Processor} of the same input and output
 * types. It doesn't expose a rich API but can be viewed as a {@link Mono} or even converted
 * to a {@link Flux}. {@link MonoProcessor} implementation may allow multiple subscribers,
 * replaying the signal they would capture to late subscribers.
 *
 * @author Simon Baslé
 */
public interface MonoProcessor<T> extends Processor<T, T> {

	/**
	 * Return a view of this {@link MonoProcessor} decorated with the full {@link Mono} API.
	 *
	 * @return a {@link Mono} that mirrors the signals of this processor.
	 */
	Mono<T> asMono();

	/**
	 * Return a view of this {@link MonoProcessor} decorated with the full {@link Flux} API.
	 *
	 * @return a {@link Flux} that mirrors the signals of this processor.
	 */
	Flux<T> asFlux();

	/**
	 * Create a {@link MonoProcessor} that will emit the <strong>first</strong> signal of
	 * the {@link Publisher} it is subscribed to. Multiple subscribers can subscribe to
	 * this processor and each will receive the signal, which is also cached for late
	 * subscribers.
	 *
	 * @param <T> the type of value emitted
	 * @return a new {@link MonoProcessor} that propagates the first signals from a source
	 * to be subscribed to later.
	 */
	static <T> MonoProcessor<T> next() {
		return MonoNextProcessor.create();
	}

	/**
	 * Create a {@link MonoProcessor} that will subscribe to the provided {@link Publisher}
	 * and emit its <strong>first</strong> signal. Multiple subscribers can subscribe to
	 * this processor and each will receive the signal, which is also cached for late
	 * subscribers.
	 *
	 * @param <T> the type of value emitted
	 * @return a new {@link MonoProcessor} that propagates the first signals from a provided source {@link Publisher}.
	 */
	static <T> MonoProcessor<T> nextOf(Publisher<T> source) {
		MonoNextProcessor<T> result;
		if (source instanceof MonoNextProcessor) {
			result = (MonoNextProcessor<T>) source;
		}
		else {
			result = new MonoNextProcessor<>(source);
		}
		result.connect();
		return result;
	}

	/**
	 * Create a standalone {@link MonoSink} that is close to a {@link MonoProcessor}, in the
	 * sense that is can be subscribed to (multiple subscribers are allowed). It also can
	 * be decorated with the full {@link Mono} API, which it doesn't expose initially.
	 * However, it cannot subscribe to a source as it is only a {@link Publisher} made for
	 * manual pushing of signals.
	 *
	 * @param <T> the type of value emitted
	 * @return a new {@link MonoSinkPublisher}
	 */
	static <T> MonoSinkPublisher<T> sink() {
		return new MonoSinkPublisher<>();
	}

}
