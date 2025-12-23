
(function(){

    // Variable para asegurar que la limpieza de la UI se haga solo una vez.
    var hasCleanedUp = false;

    // Observador para encontrar el video.
    var videoObserver = new MutationObserver(function(mutations) {
        var videoElements = document.getElementsByTagName('video');

        if (videoElements.length > 0 && !hasCleanedUp) {
            var video = videoElements[0];
            if (!video.src.includes('blob:https')) {

            }
            else if (video.src) {
                hasCleanedUp = true;
                videoObserver.disconnect();

                document.body.style.backgroundColor = 'black';
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

               video.muted = false;

               var ensureSoundInterval = setInterval(function() {
                   if (video.muted) {
                       video.muted = false;
                   }
               }, 250);

                video.play();

                document.body.appendChild(video);

                Android.receiveHtml(video.src);
            }else{
                Android.receiveHtml('No video element found');
            }
        }

    });

    // Configuración del observador de video
    var videoConfig = { attributes: true, childList: true, characterData: true, subtree: true };
    videoObserver.observe(document.body, videoConfig);


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

})();
