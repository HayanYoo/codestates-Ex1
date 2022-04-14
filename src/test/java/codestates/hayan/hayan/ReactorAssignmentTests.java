package codestates.hayan.hayan;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Locale;
import static org.assertj.core.api.Assertions.assertThat;

public class ReactorAssignmentTests {

    //1. ["Blenders", "Old", "Johnnie"] 와 "[Pride", "Monk", "Walker”] 를 순서대로 하나의 스트림으로 처리되는 로직 검증
    @Test
    public void concatWithDelay() {
        Flux<String> names1$ = Flux.just("Blenders", "Old", "Johnnie")
                .delayElements(Duration.ofSeconds(1));
        Flux<String> names2$ = Flux.just("Pride", "Monk", "Walker")
                .delayElements(Duration.ofSeconds(1));
        Flux<String> names$ = Flux.concat(names1$, names2$)
                .log();

        StepVerifier.create(names$)
                .expectSubscription()
                .expectNext("Blenders", "Old", "Johnnie", "Pride", "Monk", "Walker")
                .verifyComplete();
    }

    //2. 1~100 까지의 자연수 중 짝수만 출력하는 로직 검증
    @Test
    public void printEven() {
        Flux<Integer> flux = Flux.range(1, 100)
                .filter(i -> i % 2 == 0)
                .log();

        StepVerifier.create(flux)
                .expectNextCount(50)
                .verifyComplete();

    }


    //3. “hello”, “there” 를 순차적으로 publish하여 순서대로 나오는지 검증
    @Test
    public void printHelloThere2() {
        Flux<String> flux = Flux.just("hello", "there")
                .delayElements(Duration.ofSeconds(1))
                .log();

        StepVerifier.create(flux)
                .expectNext("hello", "there")
                .verifyComplete();
    }


    //4. 아래와 같은 객체가 전달될 때 “JOHN”, “JACK” 등 이름이 대문자로 변환되어 출력되는 로직 검증
    //Person("John", "[john@gmail.com](mailto:john@gmail.com)", "12345678")
    //Person("Jack", "[jack@gmail.com](mailto:jack@gmail.com)", "12345678")
    @Test
    public void nameToUpper() {

        class Person {
                private String name;
                private String email;
                private String password;
            
                public String getName() {
                    return name;
                }
            
                public String getEmail() {
                    return email;
                }
            
                public String getPassword() {
                    return password;
                }
            
                public Person(String name, String email, String password) {
                    this.name = name;
                    this.email = email;
                    this.password = password;
                }

                public Person changeNameToUpper(){
                        this.name = this.getName().toUpperCase();
                        return this;
                }
            }
            

        Person person1 = new Person("John", "[john@gmail.com](mailto:john@gmail.com)", "12345678");
        Person person2 = new Person("Jack", "[jack@gmail.com](mailto:jack@gmail.com)", "12345678");

        Flux<Person> flux = Flux.just(person1, person2)
                .map(i -> i.changeNameToUpper())
                .log() // log에 참조 값이 찍혀서 println으로 값 확인
                .doOnNext(i -> System.out.println(i.getName() + ", " + i.getEmail()+  ", " + i.getPassword()));

        StepVerifier.create(flux)
                .assertNext(p->assertThat(p.getName()).isEqualTo("JOHN"))
                .assertNext(p->assertThat(p.getName()).isEqualTo("JACK"))
                .verifyComplete();


    }

    //5. ["Blenders", "Old", "Johnnie"] 와 "[Pride", "Monk", "Walker”]를 압축하여 스트림으로 처리 검증
    //예상되는 스트림 결과값 ["Blenders Pride", "Old Monk", "Johnnie Walker”]
    @Test
    public void zipWith() {
        Flux<String> flux1 = Flux.just("Blenders", "Old", "Johnnie");
        Flux<String> flux2 = Flux.just("Pride", "Monk", "Walker");

        Flux<String> flux = flux1.zipWith(flux2, (a, b) -> a + " " + b)
                .log();

        StepVerifier.create(flux)
                .expectNext("Blenders Pride")
                .expectNext("Old Monk")
                .expectNext("Johnnie Walker")
                .verifyComplete();
    }

    //6. ["google", "abc", "fb", "stackoverflow”] 의 문자열 중
    // 5자 이상 되는 문자열만 대문자로 비동기로 치환하여 1번 반복하는 스트림으로 처리하는 로직 검증
    //예상되는 스트림 결과값 ["GOOGLE", "STACKOVERFLOW", "GOOGLE", "STACKOVERFLOW"]
    @Test
    public void wordLenGreaterThan5ToUpperCaseAndRepeat() {
        Flux<String> flux = Flux.just("google", "abc", "fb", "stackoverflow")
                .filter(it -> it.length() >= 5)
                .flatMap(it -> Mono.just(it.toUpperCase(Locale.ROOT)))
                .repeat(1)
                .subscribeOn(Schedulers.boundedElastic())
                .log();

        StepVerifier.create(flux)
                .expectNextCount(4)
                .verifyComplete();

    }
}



