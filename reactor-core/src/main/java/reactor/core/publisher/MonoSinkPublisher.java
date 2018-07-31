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

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.LongConsumer;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.CoreSubscriber;
import reactor.core.Disposable;
import reactor.util.annotation.Nullable;
import reactor.util.context.Context;

/**
 * The manual equivalent to a {@link MonoProcessor}, a {@link MonoSinkPublisher} is
 * a {@link Publisher} that is programmatically driven, using the {@link MonoSink} API.
 * {@link MonoSinkPublisher} implementations may allow multiple subscribers,
 * replaying the signal they would capture to late subscribers.
 *
 * @param <T> the type of value emitted
 * @author Simon Baslé
 */
public class MonoSinkPublisher<T> implements MonoSink<T>, Publisher<T> {

	volatile SinkInner<T>[] subscribers;

	@SuppressWarnings("rawtypes")
	static final AtomicReferenceFieldUpdater<MonoSinkPublisher, SinkInner[]>
			SUBSCRIBERS =
			AtomicReferenceFieldUpdater.newUpdater(MonoSinkPublisher.class,
					SinkInner[].class,
					"subscribers");

	@SuppressWarnings("rawtypes")
	static final SinkInner[] EMPTY = new SinkInner[0];

	@SuppressWarnings("rawtypes")
	static final SinkInner[] TERMINATED = new SinkInner[0];

	volatile int done;
	AtomicIntegerFieldUpdater<MonoSinkPublisher> DONE =
			AtomicIntegerFieldUpdater.newUpdater(MonoSinkPublisher.class, "done");

	@Nullable
	T         value;
	Throwable error;

	MonoSinkPublisher() {
		SUBSCRIBERS.lazySet(this, EMPTY);
	}

	@Override
	public Context currentContext() {
		return Operators.multiSubscribersContext(subscribers);
	}

	@Override
	public void success() {
		success(null);
	}

	@Override
	public void success(@Nullable T value) {
		if (DONE.compareAndSet(this, 0, 1)) {
			this.value = value;

			@SuppressWarnings("unchecked")
			SinkInner<T>[] subs = SUBSCRIBERS.getAndSet(this, TERMINATED);
			if (value == null) {
				for (SinkInner<T> s : subs) {
					s.onComplete();
				}
			}
			else {
				for (SinkInner<T> s : subs) {
					s.complete(value);
				}
			}
		}
	}

	@Override
	public void error(Throwable e) {
		if (DONE.compareAndSet(this, 0, 1)) {
			this.error = e;

			@SuppressWarnings("unchecked")
			SinkInner<T>[] subs = SUBSCRIBERS.getAndSet(this, TERMINATED);
			for (SinkInner<T> s : subs) {
				s.onError(e);
			}
		}
	}

	@Override
	public void subscribe(Subscriber<? super T> subscriber) {
		CoreSubscriber<? super T> actual = Operators.toCoreSubscriber(subscriber);
		SinkInner<T> as = new SinkInner<>(actual, this);
		actual.onSubscribe(as);
		if (add(as)) {
			if (as.isCancelled()) {
				remove(as);
			}
		}
		else {
			Throwable ex = error;
			if (ex != null) {
				actual.onError(ex);
			}
			else {
				T v = value;
				if (v != null) {
					as.complete(v);
				}
				else {
					as.onComplete();
				}
			}
		}
	}

	public Mono<T> asMono() {
		return Mono.fromDirect(this);
	}

	@Override

	public MonoSink<T> onRequest(LongConsumer consumer) {
		//FIXME should the standalone version implement this?
		return this;
	}

	@Override
	public MonoSink<T> onCancel(Disposable d) {
		//FIXME should the standalone version implement this?
		return this;
	}

	@Override
	public MonoSink<T> onDispose(Disposable d) {
		//FIXME should the standalone version implement this?
		return this;
	}

	boolean add(SinkInner<T> ps) {
		for (;;) {
			SinkInner<T>[] a = subscribers;

			if (a == TERMINATED) {
				return false;
			}

			int n = a.length;
			@SuppressWarnings("unchecked")
			SinkInner<T>[] b = new SinkInner[n + 1];
			System.arraycopy(a, 0, b, 0, n);
			b[n] = ps;

			if (SUBSCRIBERS.compareAndSet(this, a, b)) {
				return true;
			}
		}
	}

	@SuppressWarnings("unchecked")
	void remove(SinkInner<T> ps) {
		for (;;) {
			SinkInner<T>[] a = subscribers;
			int n = a.length;
			if (n == 0) {
				return;
			}

			int j = -1;
			for (int i = 0; i < n; i++) {
				if (a[i] == ps) {
					j = i;
					break;
				}
			}

			if (j < 0) {
				return;
			}

			SinkInner<T>[] b;

			if (n == 1) {
				b = EMPTY;
			} else {
				b = new SinkInner[n - 1];
				System.arraycopy(a, 0, b, 0, j);
				System.arraycopy(a, j + 1, b, j, n - j - 1);
			}
			if (SUBSCRIBERS.compareAndSet(this, a, b)) {
				return;
			}
		}
	}

	final static class SinkInner<T> extends Operators.MonoSubscriber<T, T> {
		
		final MonoSinkPublisher<T> parent;

		SinkInner(CoreSubscriber<? super T> actual, MonoSinkPublisher<T> parent) {
			super(actual);
			this.parent = parent;
		}

		@Override
		public void cancel() {
			if (STATE.getAndSet(this, CANCELLED) != CANCELLED) {
				parent.remove(this);
			}
		}

		@Override
		public void onComplete() {
			if (!isCancelled()) {
				actual.onComplete();
			}
		}

		@Override
		public void onError(Throwable t) {
			if (!isCancelled()) {
				actual.onError(t);
			}
		}

		@Override
		public Object scanUnsafe(Attr key) {
			if (key == Attr.PARENT) {
				return parent;
			}
			return super.scanUnsafe(key);
		}
	}
}
