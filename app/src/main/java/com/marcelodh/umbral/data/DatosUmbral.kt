package com.marcelodh.umbral.data

import android.content.Context

class DatosUmbral private constructor(context: Context) : BaseDatosUmbral(context) {
    companion object {
        private var instancia: DatosUmbral? = null

        @Synchronized
        fun obtener(context: Context): DatosUmbral =
            instancia ?: DatosUmbral(context.applicationContext).also { instancia = it }
    }
}
