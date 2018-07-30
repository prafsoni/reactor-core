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

import java.util.function.LongConsumer;

import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.Disposable;
import reactor.util.context.Context;

/**
 * A simplified {@link Processor} with the same in and out types, that can be used as both
 * a {@link Subscriber} to an existing source and as a {@link FluxSink} (which allows to
 * augment the source with manually injected signals). This is a good alternative to the
 * more bloated {@link FluxProcessor}.
 * <p>
 * The first terminal signal will terminate the processor and be replayed to downstream(s)
 * {@link Subscriber Subscribers}. As a consequence, pay attention to quickly completing
 * source {@link org.reactivestreams.Publisher} if you plan to also publish manual data to
 * the processor.
 *
 * @author Simon Baslé
 */
public class FluxSinkProcessor<T> implements Processor<T, T>, FluxSink<T> {

	final FluxProcessor<T, T> underlying;
	final FluxSink<T> sink;

	FluxSinkProcessor(FluxProcessor<T, T> underlying) {
		this.underlying = underlying;
		this.sink = underlying.sink();
	}

	FluxSinkProcessor(FluxProcessor<T, T> underlying, OverflowStrategy overflowStrategy) {
		this.underlying = underlying;
		this.sink = underlying.sink(overflowStrategy);
	}

	/**
	 * Return the raw {@link FluxProcessor} that is backing this {@link FluxSinkProcessor}.
	 * The raw processor is not specifically guarded for use with bot its sink and an upstream
	 * {@link org.reactivestreams.Publisher}, and exposes a much more complex API (including
	 * that of {@link Flux}).
	 *
	 * @return the backing unprotected {@link FluxProcessor}
	 */
	public final FluxProcessor<T, T> getRawProcessor() {
		return underlying;
	}

	/**
	 * Expose this processor as a {@link Flux}.
	 *
	 * @return A {@link Flux} representation of the {@link Processor}
	 */
	public final Flux<T> asFlux() {
		return underlying;
	}

	/**
	 * Expose this processor as a {@link Mono}. Similar to calling {@link #asFlux()} then
	 * {@link Flux#next()}.
	 *
	 * @return A {@link Mono} representation of the {@link Processor}
	 */
	public final Mono<T> asMono() {
		return underlying.next();
	}

	@Override
	public FluxSink<T> next(T t) {
		return sink.next(t);
	}

	@Override
	public void onNext(T t) {
		sink.next(t);
	}

	@Override
	public void complete() {
		sink.complete();
	}

	@Override
	public void onComplete() {
		sink.complete();
	}

	@Override
	public void error(Throwable e) {
		sink.error(e);
	}

	@Override
	public void onError(Throwable throwable) {
		sink.error(throwable);
	}

	@Override
	public void onSubscribe(Subscription subscription) {
		if (sink.isCancelled()) {
			subscription.cancel();
		}
		else {
			subscription.request(Long.MAX_VALUE);
		}
	}

	@Override
	public Context currentContext() {
		return sink.currentContext();
	}

	@Override
	public long requestedFromDownstream() {
		return sink.requestedFromDownstream();
	}

	@Override
	public boolean isCancelled() {
		return sink.isCancelled();
	}

	@Override
	public FluxSink<T> onRequest(LongConsumer consumer) {
		return sink.onRequest(consumer);
	}

	@Override
	public FluxSink<T> onCancel(Disposable d) {
		return sink.onCancel(d);
	}

	@Override
	public FluxSink<T> onDispose(Disposable d) {
		return sink.onDispose(d);
	}

	@Override
	public void subscribe(Subscriber<? super T> subscriber) {
		underlying.subscribe(subscriber);
	}
}
