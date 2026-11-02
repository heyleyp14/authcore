package com.pamplona.authcore.application.ports.out;

import java.util.List;

/** Trozo de una lista y el total de elementos que hay antes de paginar. */
public record Pagina<T>(List<T> contenido, long total, int pagina, int tamano) {

    public int totalPaginas() {
        return tamano <= 0 ? 0 : (int) Math.ceil((double) total / tamano);
    }
}
