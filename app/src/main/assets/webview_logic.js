(function(){
    if (window.location.hostname.includes('instagram.com')) {
    // Variable para asegurar que la limpieza de la UI se haga solo una vez.
    var hasCleanedUp = false;

    // Observador para encontrar el video.
    var videoObserver = new MutationObserver(function(mutations) {
        var videoElements = document.getElementsByTagName('video');

        if (videoElements.length > 0 && !hasCleanedUp) {
            var video = videoElements[0];

            // Verificamos que el video tenga una URL (puede ser blob: o https:)
            if (video.src) {
                hasCleanedUp = true; // Marcamos que ya hemos procesado el video.
                videoObserver.disconnect(); // Dejamos de buscar más videos.

                // --- ¡AQUÍ ESTÁ LA MAGIA! ---
                // 1. Aislamos el video y lo estilizamos para que ocupe todo.

                // Hacemos que el body sea negro y ocultamos el overflow.

                document.body.style.margin = '0';
                document.body.style.overflow = 'hidden';

                // Borramos todos los hijos directos del body.
                while (document.body.firstChild) {
                    document.body.removeChild(document.body.firstChild);
                }

                // Estilos para el video.
                video.style.position = 'fixed';
                video.style.top = '0';
                video.style.left = '0';
                video.style.width = '100vw'; // 100% del ancho de la ventana
                video.style.height = 'calc(100vh - 120px)';
                video.style.objectFit = 'contain'; // Asegura que se vea completo sin distorsión.
                video.setAttribute('controls', 'true'); // Forzamos a que muestre los controles.
               // Opcional: intentamos darle al play.

               video.muted = false;


                   var ensureSoundInterval = setInterval(function() {
                        if (video.muted) {
                            video.muted = false;
                        }
                    }, 250)

                video.play();

                // 2. Re-insertamos solo el video en el body limpio.
                document.body.appendChild(video);



                // 3. Notificamos a Kotlin la URL real (blob: o https:)
                //    para el botón de descarga si es una URL descargable.
                Android.receiveHtml(video.src);
            }
        }

    });

    // Configuración del observador de video
    var videoConfig = { attributes: true, childList: true, characterData: true, subtree: true };
    videoObserver.observe(document.body, videoConfig);
    }

})();

(function() {
    const elements = document.querySelectorAll(".Content EmbedFrame");

    elements.forEach(element => {
        element.click();
    });
})();

(function() {

    var observer = new MutationObserver(function(mutations) {
        var videoElements = document.getElementsByTagName('video');
    if (videoElements.length > 0) {
        var video = videoElements[0];
        var videoInfo = {
            src: video.src,
            width: video.width,
            height: video.height,
            autoplay: video.autoplay,
            controls: video.controls
        };

        Android.receiveHtml(JSON.stringify(videoInfo.src));
    } else {
        Android.receiveHtml('No video element found');
    }
    });

    var config = { attributes: true, childList: true, characterData: true, subtree: true };
    observer.observe(document.body, config);

    // --- NUEVO CÓDIGO PARA BUSCAR EL TEXTO "Ver en Instagram" ---
     function findInstagramClass() {
        // Buscamos un elemento que contenga la clase 'WatchOnInstagram'.
        // querySelector es muy eficiente. El '.' indica que es una clase.
        const element = document.querySelector('.WatchOnInstagram');

        // Si el elemento existe...
        if (element) {
            // ¡Clase encontrada! Notificamos a Kotlin.
            // Pasamos el texto del elemento por si es útil.
            Android.foundWatchOnInstagram(element.textContent);

            // Detenemos el observador para no seguir buscando innecesariamente.
            classObserver.disconnect();
        }
    }

    // Usamos un MutationObserver por si el elemento se añade a la página dinámicamente.
    var classObserver = new MutationObserver(function(mutations) {
        findInstagramClass();
    });
    var classConfig = { childList: true, subtree: true };
    classObserver.observe(document.body, classConfig);

    // Ejecutamos la búsqueda una vez al inicio también, por si el elemento ya está presente.
    findInstagramClass();
})();


