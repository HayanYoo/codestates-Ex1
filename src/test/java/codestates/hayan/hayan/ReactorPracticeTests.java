package codestates.hayan.hayan;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class ReactorPracticeTests {
    @Test
    void testError() {
        Flux flux = Flux.just("thing1", "thing2")
                .concatWith(Mono.error(new IllegalArgumentException("boom")))
                .log();

        StepVerifier.create(flux)
                .expectNext("thing1")
                .expectNext("thing2")
                .expectError()
                .verify();
    }



    @Test
    public void testBackPressure() {
        Flux.just(1, 2, 3, 4)
                .log()
                .subscribe(new Subscriber() {
                    private Subscription s;
                    int onNextAmount;

                    @Override
                    public void onSubscribe(Subscription s) {
                        this.s = s;
                        s.request(2);
                    }

                    @Override
                    public void onNext(Object o) {
                        onNextAmount++;
                        if (onNextAmount % 2 == 0) {
                            s.request(2);
                        }
                    }

                    @Override
                    public void onError(Throwable t) {}

                    @Override
                    public void onComplete() {}
                });
    }



    @Test
    public void testMap() {
        Flux<Integer> flux = Flux.just(1, 2, 3);
        Flux<Integer> flux2 = flux.map(i -> i * 10)
                .log();

        StepVerifier.create(flux2)
                .expectNext(10, 20, 30)
                .verifyComplete();
    }



    @Test
    public void testFlatMap() {
        Flux<Integer> flux = Flux.just(1, 2, 3);
        Flux<Integer> flux2 = flux.flatMap(i -> Mono.just(i * 10))
                .log();

        StepVerifier.create(flux2)
                .expectNextCount(flux.count().block())
                .verifyComplete();
    }



    @Test
    public void testScheduler() {
        Flux<Integer> flux = Flux.just(1, 2, 3);
        Flux<Integer> flux2 = flux.flatMap(i -> Mono.just(i * 10))
                .publishOn(Schedulers.boundedElastic())
                .filter(i -> i/10 == 2)
                .publishOn(Schedulers.parallel())
//                .subscribeOn(Schedulers.boundedElastic())
                .log();

        for (int i = 0; i < 10; i++) {
            StepVerifier.create(flux2)
                    .expectNext(20)
                    .verifyComplete();
        }
    }



    @Test
    public void zipTest() {
        Flux<Tuple2<Integer, Integer>> flux = Flux.just(1, 2, 3, 4)
                .log()
                .map(i -> i * 2)
                .zipWith(Flux.range(0, Integer.MAX_VALUE))
                .log();

        StepVerifier.create(flux)
                .expectNextCount(4)
                .verifyComplete();
    }



    @Test
    public void groupByTest() {
        List<Integer> intList = new ArrayList<>();
        Random r = new Random();
        for (int i=0; i<100; i++) {
            intList.add(r.nextInt(10));
        }

        Flux<Integer> flux = Flux.fromIterable(intList);

        flux.groupBy(i -> i % 10) //key : predicate value, value : predicate 로 필터링 된 Flux<Object>
                .doOnNext(g -> System.out.println(g.key() + " :" + g.collectList()))
                .subscribe();
    }



    //1. ["Blenders", "Old", "Johnnie"] 와 "[Pride", "Monk", "Walker"] 를
    // 순서대로 하나의 스트림으로 처리되는 로직 검증

    @Test
    public void concatWithDelay() {
        Flux<String> names1 = Flux.just("Blenders", "Old", "Johnnie")
                .delayElements(Duration.ofSeconds(1));
        Flux<String> names2 = Flux.just("Pride", "Monk", "Walker")
                .delayElements(Duration.ofSeconds(1));
        Flux<String> names = Flux.concat(names1, names2)
                .log();

        StepVerifier.create(names)
                .expectSubscription()
                .expectNext("Blenders", "Old", "Johnnie", "Pride", "Monk", "Walker")
                .verifyComplete();
    }

}
