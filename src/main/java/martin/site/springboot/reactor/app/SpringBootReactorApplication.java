package martin.site.springboot.reactor.app;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.MaximizeAction;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import martin.site.springboot.reactor.app.models.Comentarios;
import martin.site.springboot.reactor.app.models.Usuario;
import martin.site.springboot.reactor.app.models.UsuarioComentarios;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class SpringBootReactorApplication implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(SpringBootReactorApplication.class);
	public static void main(String[] args) {
		SpringApplication.run(SpringBootReactorApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		ejemploContraPresion();
	}
	public void ejemploContraPresion() {
		Flux.range(1, 10)
		.log()
		.limitRate(2)
		.subscribe(
				/*new Subscriber<Integer>() {
			private Subscription s;
			private Integer limite = 2;
			private Integer consumido = 0;
			@Override
			public void onSubscribe(Subscription s) {
				this.s = s;
				s.request(limite);
			}

			@Override
			public void onNext(Integer t) {
				log.info(t.toString());
				consumido++;
				if(consumido == limite) {
					consumido = 0;
					s.request(limite);
				}
			}

			@Override
			public void onError(Throwable t) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onComplete() {
				// TODO Auto-generated method stub
				
			}
		}*/
		);
	}
	
	public void ejemploIntervalDesdeCreate() throws InterruptedException {
		Flux.create(emitter -> {
			Timer timer = new Timer();
			timer.schedule(new TimerTask() {
				private Integer contador = 0;
				@Override
				public void run() {
					emitter.next(++contador);
					if(contador == 10) {
						timer.cancel();
						emitter.complete();
					}
					if(contador == 5) {
						timer.cancel();
						emitter.error(new InterruptedException("Error, se ha detenido el Flux en 5"));
					}
				}
			}, 1000, 1000);
		})
		.subscribe(next -> log.info(next.toString()), error -> log.info(error.getMessage()));
	}
	
	public void ejemploIntervalInfinito() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		Flux.interval(Duration.ofSeconds(1))
		.doOnTerminate(() -> latch.countDown())
		.flatMap(i -> {
			if(i >= 5) {
				return Flux.error(new InterruptedException("Solo hasta 5"));
			} 
			return Flux.just(i);
		})
		.map(i -> "Hola " + i)
		.retry(2)
		.subscribe(s -> log.info(s), 
				e -> log.error(e.getMessage()),
				() -> log.info("Hemos Terminado"));
		
		latch.await();
	}
	
	public void ejemploDelayElements() {
		Flux<Integer> rango = Flux.range(1, 12)
				.delayElements(Duration.ofSeconds(1))
				.doOnNext(i -> log.info(i.toString()));
		rango.subscribe();
		
		try {
			Thread.sleep(13000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void ejemploInterval() {
		Flux<Integer> rango = Flux.range(1, 12);
		Flux<Long> retraso = Flux.interval(Duration.ofSeconds(1));
		rango.zipWith(retraso, (ra,re) -> ra)
		.doOnNext(i -> log.info(i.toString()))
		.blockLast();
	}
	
	public void ejemploZipWithRangos() {
		Flux<Integer> rangos = Flux.range(0, 4);
		Flux.just(1,2,3,4)
		.map(i -> (i*2))
		.zipWith(rangos, (uno, dos) -> String.format("Primer Flux: %d, Segundo Flux: %d", uno, dos))
		.subscribe(texto -> log.info(texto));
	}
	
	public void ejemploUsuariosComentariosZipWithForma2() {
		Mono<Usuario> usuarioMono = Mono.fromCallable(() -> new Usuario("Jhon", "Doe"));
		Mono<Comentarios> comentariosUsuarioMono = Mono.fromCallable(() -> {
			Comentarios comentarios = new Comentarios();
			comentarios.addComentario("Hola");
			comentarios.addComentario("Hola 2");
			comentarios.addComentario("Hola 3");
			return comentarios;
		});
		
		Mono<UsuarioComentarios> usuarioConcomentarios = usuarioMono
				.zipWith(comentariosUsuarioMono)
				.map(tuple -> {
					Usuario usuario = tuple.getT1();
					Comentarios comentarios = tuple.getT2();
					return new UsuarioComentarios(usuario, comentarios);
				});
		usuarioConcomentarios.subscribe(uc -> log.info(uc.toString()));
	}
	
	public void ejemploUsuariosComentariosZipWith() {
		Mono<Usuario> usuarioMono = Mono.fromCallable(() -> new Usuario("Jhon", "Doe"));
		Mono<Comentarios> comentariosUsuarioMono = Mono.fromCallable(() -> {
			Comentarios comentarios = new Comentarios();
			comentarios.addComentario("Hola");
			comentarios.addComentario("Hola 2");
			comentarios.addComentario("Hola 3");
			return comentarios;
		});
		
		Mono<UsuarioComentarios> usuarioConcomentarios = usuarioMono
				.zipWith(comentariosUsuarioMono, (usuario,comentarios) -> new UsuarioComentarios(usuario, comentarios));
		usuarioConcomentarios.subscribe(uc -> log.info(uc.toString()));
	}
	
	public void ejemploUsuariosComentariosFlatMap() {
		Mono<Usuario> usuarioMono = Mono.fromCallable(() -> new Usuario("Jhon", "Doe"));
		Mono<Comentarios> comentariosUsuarioMono = Mono.fromCallable(() -> {
			Comentarios comentarios = new Comentarios();
			comentarios.addComentario("Hola");
			comentarios.addComentario("Hola 2");
			comentarios.addComentario("Hola 3");
			return comentarios;
		});
		
		usuarioMono.flatMap(u -> comentariosUsuarioMono.map(c -> new UsuarioComentarios(u, c)))
		.subscribe(uc -> log.info(uc.toString()));
	}
	
	public void ejemploCollectList() throws Exception {
		List<Usuario> usuariosList = new ArrayList<>();
		usuariosList.add(new Usuario("Andres", "Guzman"));
		usuariosList.add(new Usuario("Pedro", "Fulan"));
		usuariosList.add(new Usuario("Maria", "Sultana"));
		usuariosList.add(new Usuario("Diego", "Mengano"));
		usuariosList.add(new Usuario("Juan", "Barreto"));
		usuariosList.add(new Usuario("Bruce", "Lee"));
		usuariosList.add(new Usuario("Bruce", "Willis"));

		Flux.fromIterable(usuariosList) 
		.collectList()
		.subscribe(lista-> log.info(lista.toString()));
	}
	
	public void ejemploToString() throws Exception {
		List<Usuario> usuariosList = new ArrayList<>();
		usuariosList.add(new Usuario("Andres", "Guzman"));
		usuariosList.add(new Usuario("Pedro", "Fulan"));
		usuariosList.add(new Usuario("Maria", "Sultana"));
		usuariosList.add(new Usuario("Diego", "Mengano"));
		usuariosList.add(new Usuario("Juan", "Barreto"));
		usuariosList.add(new Usuario("Bruce", "Lee"));
		usuariosList.add(new Usuario("Bruce", "Willis"));

		Flux.fromIterable(usuariosList) //Flux.just("Andres Guzman","Pedro Fulan","Maria Sultana","Diego Mengano","Juan Barreto","Bruce Lee","Bruce Willis");			
		.map(usuario -> usuario.getNombre().toUpperCase().concat(" ").concat(usuario.getApellido().toUpperCase()))
		.flatMap(nombre -> {
			if(nombre.contains("BRUCE")) {
				return Mono.just(nombre);
			} else {
				return Mono.empty();
			}
		})
				.map(nombre -> {
					return nombre.toLowerCase();
				})
				.subscribe(u-> log.info(u.toString()));
	}
	
	public void ejemploFlatMap() throws Exception {
		List<String> usuariosList = new ArrayList<>();
		usuariosList.add("Andres Guzman");
		usuariosList.add("Pedro Fulan");
		usuariosList.add("Maria Sultana");
		usuariosList.add("Diego Mengano");
		usuariosList.add("Juan Barreto");
		usuariosList.add("Bruce Lee");
		usuariosList.add("Bruce Willis");

		Flux.fromIterable(usuariosList) //Flux.just("Andres Guzman","Pedro Fulan","Maria Sultana","Diego Mengano","Juan Barreto","Bruce Lee","Bruce Willis");			
		.map(nombre -> new Usuario(nombre.split(" ")[0].toUpperCase(), nombre.split(" ")[1].toUpperCase()))
		.flatMap(usuario -> {
			if(usuario.getNombre().equalsIgnoreCase("bruce")) {
				return Mono.just(usuario);
			} else {
				return Mono.empty();
			}
		})
				.map(usuario -> {
					String nombre = usuario.getNombre().toLowerCase();
					usuario.setNombre(nombre);
					return usuario;
				})
				.subscribe(u-> log.info(u.toString()));
	}

	
	public void ejemploIterable() throws Exception {
		List<String> usuariosList = new ArrayList<>();
		usuariosList.add("Andres Guzman");
		usuariosList.add("Pedro Fulan");
		usuariosList.add("Maria Sultana");
		usuariosList.add("Diego Mengano");
		usuariosList.add("Juan Barreto");
		usuariosList.add("Bruce Lee");
		usuariosList.add("Bruce Willis");
		
		Flux<String> nombres = Flux.fromIterable(usuariosList); //Flux.just("Andres Guzman","Pedro Fulan","Maria Sultana","Diego Mengano","Juan Barreto","Bruce Lee","Bruce Willis");
				
		Flux<Usuario> usuarios = nombres.map(nombre -> new Usuario(nombre.split(" ")[0].toUpperCase(), nombre.split(" ")[1].toUpperCase()))
				.filter(usuario -> usuario.getNombre().equalsIgnoreCase("bruce"))
				.doOnNext(usuario -> {
					if(usuario == null) {
						throw new RuntimeException("nombre no puede ser vacio");
					}
					System.out.println(usuario.getNombre().concat(" ").concat(usuario.getApellido()));
				})
				.map(usuario -> {
					String nombre = usuario.getNombre().toLowerCase();
					usuario.setNombre(nombre);
					return usuario;
				});
		
		usuarios.subscribe(e-> log.info(e.toString()),
				error-> log.error(error.getMessage()),
				new Runnable() {
					
					@Override
					public void run() {
						log.info("Ha finalizado la ejecucion del observable con exito");
					}
				});
	}

}
