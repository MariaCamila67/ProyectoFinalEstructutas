<script setup>
// Vista del MVP. Las acciones se ejecutan en el controlador y en el backend Java.
import { crearControladorTaller } from '../controlador/TallerController.js'

const {
  pagina, mensaje, datos, acceso, clienteForm, buscarId, autoForm, reparacionForm, pagoForm,
  consultaPlaca, consultaEstado, busquedaFacturas, colaMantenimiento, colaAccidentes,
  autenticar, registrarCliente, buscarCliente, registrarAutomovil, cargarRepuestos,
  crearReparacion, ejecutarPuestaAPunto, procesarPago, generarFactura, consultarEstado,
  buscarFacturasPorPlaca, verFacturasPagadas, atenderSiguiente, formatoDinero,
} = crearControladorTaller()

const pasos = [
  ['F1', 'Autenticar mecanico'], ['F2', 'Registrar cliente'], ['F3', 'Registrar automovil'],
  ['F4', 'Cargar repuestos'], ['F5', 'Crear reparacion'], ['F6', 'Puesta a punto'],
  ['F7', 'Procesar pago'], ['F8', 'Generar factura'], ['F9', 'Consultar facturas'],
  ['F10', 'Orden de accidentes'],
]
</script>

<template>
  <main class="pagina">
    <h1>Taller de automoviles</h1>
    <nav>
      <button v-for="paso in pasos" :key="paso[0]" :class="{ activo: pagina === paso[0] }" @click="pagina = paso[0]">{{ paso[0] }}. {{ paso[1] }}</button>
    </nav>
    <p v-if="mensaje" class="mensaje">{{ mensaje }} <button @click="mensaje = ''">Cerrar</button></p>

    <section v-if="pagina === 'F1'">
      <h2>Autenticar mecanico</h2>
      <form class="formulario" @submit.prevent="autenticar">
        <label>Nombre <input v-model="acceso.nombre" required /></label>
        <label>Contrasena <input v-model="acceso.contrasena" type="password" required /></label>
        <button>Ingresar</button>
      </form>
      <p v-if="datos.mecanico">Sesion activa: {{ datos.mecanico.nombre }}</p>
    </section>

    <section v-if="pagina === 'F2'">
      <h2>Registrar o buscar cliente</h2>
      <form class="formulario" @submit.prevent="registrarCliente">
        <label>Documento <input v-model="clienteForm.id" required /></label>
        <label>Nombre <input v-model="clienteForm.nombre" required /></label>
        <label>Telefono <input v-model="clienteForm.telefono" placeholder="3001234567" required /></label>
        <label>Ingresos mensuales <input v-model="clienteForm.ingresos" type="number" min="0" /></label>
        <label><input v-model="clienteForm.pedirCredito" type="checkbox" /> Solicitar credito</label>
        <button>Registrar cliente</button>
      </form>
      <h3>Buscar cliente por documento</h3>
      <label>Documento <input v-model="buscarId" /></label><button @click="buscarCliente">Buscar</button>
      <p v-if="datos.clienteActivo">Cliente: {{ datos.clienteActivo.nombre }}. Saldo: {{ formatoDinero(datos.clienteActivo.saldo) }}</p>
      <p v-if="datos.clienteActivo">Credito aprobado: {{ datos.clienteActivo.credito ? 'Si' : 'No' }}</p>
    </section>

    <section v-if="pagina === 'F3'">
      <h2>Registrar automovil</h2>
      <p v-if="datos.clienteActivo">Cliente: {{ datos.clienteActivo.nombre }}</p>
      <p v-else>Busca o registra un cliente en F2 primero.</p>
      <form class="formulario" @submit.prevent="registrarAutomovil">
        <label>Placa <input v-model="autoForm.placa" placeholder="ABC123" required /></label>
        <label>Marca <input v-model="autoForm.marca" required /></label>
        <label>Modelo <input v-model="autoForm.modelo" required /></label>
        <label>Anio <input v-model="autoForm.anio" type="number" required /></label>
        <label>Tipo <select v-model="autoForm.tipo"><option>Gasolina</option><option>Electrico</option><option>Hibrido</option></select></label>
        <label v-if="autoForm.tipo === 'Gasolina'">Cilindraje <select v-model="autoForm.cilindraje"><option>C4</option><option>C6</option><option>C12</option><option>C16</option></select></label>
        <label v-if="autoForm.tipo === 'Electrico'">Autonomia <input v-model="autoForm.autonomia" type="number" min="0" max="100" /></label>
        <button>Registrar automovil</button>
      </form>
      <p v-if="datos.automovilActivo">Automovil activo: {{ datos.automovilActivo.placa }} - {{ datos.automovilActivo.marca }} {{ datos.automovilActivo.modelo }}</p>
    </section>

    <section v-if="pagina === 'F4'">
      <h2>Cargar repuestos</h2>
      <button @click="cargarRepuestos">Cargar repuestos</button>
      <table><thead><tr><th>Repuesto</th><th>Proveedor</th><th>Cantidad disponible</th></tr></thead>
        <tbody><tr v-for="item in datos.inventario" :key="item.nombre"><td>{{ item.nombre }}</td><td>{{ item.proveedor }}</td><td>{{ item.cantidad }}</td></tr></tbody>
      </table>
      <p v-if="datos.inventario.length === 0">Todavia no se cargan repuestos.</p>
    </section>

    <section v-if="pagina === 'F5'">
      <h2>Crear reparacion</h2>
      <p v-if="datos.clienteActivo">Cliente: {{ datos.clienteActivo.nombre }}</p>
      <p v-if="datos.automovilActivo">Automovil: {{ datos.automovilActivo.placa }} - {{ datos.automovilActivo.marca }} {{ datos.automovilActivo.modelo }}</p>
      <form class="formulario" @submit.prevent="crearReparacion">
        <label>Tipo de ingreso <select v-model="reparacionForm.tipo"><option value="MANTENIMIENTO">Mantenimiento</option><option value="ACCIDENTE">Accidente</option></select></label>
        <label v-if="reparacionForm.tipo === 'ACCIDENTE'">Nivel de dano <select v-model.number="reparacionForm.dano"><option :value="1">1 leve</option><option :value="2">2</option><option :value="3">3</option><option :value="4">4</option><option :value="5">5 critico</option></select></label>
        <label>Valor de reparacion <input v-model="reparacionForm.valor" type="number" min="0" required /></label>
        <h3>Repuestos utilizados</h3>
        <label v-for="item in datos.inventario" :key="item.nombre">{{ item.nombre }} ({{ item.cantidad }} disponibles)
          <input v-model.number="reparacionForm.repuestos[item.nombre]" type="number" min="0" :max="item.cantidad" />
        </label>
        <label v-if="datos.clienteActivo?.credito"><input v-model="reparacionForm.credito" type="checkbox" /> Dejar a credito</label>
        <button>Crear reparacion y agregar a cola</button>
      </form>
      <p v-if="datos.reparacionActiva">Turno activo: #{{ datos.reparacionActiva.turno }}</p>
    </section>

    <section v-if="pagina === 'F6'">
      <h2>Ejecutar puesta a punto</h2>
      <p v-if="datos.reparacionActiva">Vehiculo: {{ datos.reparacionActiva.placa }} ({{ datos.reparacionActiva.tipoAuto }})</p>
      <p v-else>No hay reparacion activa.</p>
      <button :disabled="!datos.reparacionActiva" @click="ejecutarPuestaAPunto">Ejecutar puesta a punto</button>
      <p v-if="datos.reparacionActiva?.resumenMantenimiento">{{ datos.reparacionActiva.resumenMantenimiento }}</p>
    </section>

    <section v-if="pagina === 'F7'">
      <h2>Procesar pago</h2>
      <p v-if="datos.clienteActivo">Cliente: {{ datos.clienteActivo.nombre }}. Saldo: {{ formatoDinero(datos.clienteActivo.saldo) }}</p>
      <p v-if="datos.reparacionActiva && pagoForm.tipoPago === 'actual'">Valor: {{ formatoDinero(datos.reparacionActiva.valor) }}</p>
      <form class="formulario" @submit.prevent="procesarPago">
        <label>Que desea pagar? <select v-model="pagoForm.tipoPago"><option value="actual">Reparacion actual</option><option value="saldo">Saldo pendiente</option></select></label>
        <label v-if="pagoForm.tipoPago === 'saldo'">Abono <input v-model="pagoForm.abono" type="number" min="1" required /></label>
        <label>Medio de pago <select v-model="pagoForm.medio"><option>Efectivo</option><option>Tarjeta</option></select></label>
        <label v-if="pagoForm.medio === 'Efectivo'">Dinero recibido <input v-model="pagoForm.recibido" type="number" min="1" required /></label>
        <label v-if="pagoForm.medio === 'Tarjeta'">Numero de cuotas <input v-model="pagoForm.cuotas" type="number" min="1" required /></label>
        <button>Procesar pago</button>
      </form>
      <p v-if="datos.ultimoPago">Pago: {{ formatoDinero(datos.ultimoPago.monto) }}. Cambio: {{ formatoDinero(datos.ultimoPago.cambio) }}</p>
    </section>

    <section v-if="pagina === 'F8'">
      <h2>Generar factura y autorizar salida</h2>
      <p v-if="datos.ultimaFactura">Factura {{ datos.ultimaFactura.placa }} - {{ datos.ultimaFactura.cliente }} - {{ formatoDinero(datos.ultimaFactura.valor) }} - {{ datos.ultimaFactura.estado }}</p>
      <p v-else>Genera la factura despues del pago o si la reparacion quedo a credito.</p>
      <button @click="generarFactura">Generar factura y registrar salida</button>
      <pre v-if="datos.ultimaFactura">Factura #{{ datos.ultimaFactura.id }}
Placa: {{ datos.ultimaFactura.placa }}
Cliente: {{ datos.ultimaFactura.cliente }}
Valor: {{ formatoDinero(datos.ultimaFactura.valor) }}
Fecha: {{ datos.ultimaFactura.fecha }}
Estado: {{ datos.ultimaFactura.estado }}</pre>
    </section>

    <section v-if="pagina === 'F9'">
      <h2>Consultar facturas en MongoDB</h2>
      <label>Placa <input v-model="consultaPlaca" placeholder="ABC123" /></label>
      <button @click="consultarEstado">Consultar estado</button>
      <button @click="buscarFacturasPorPlaca">Buscar por placa</button>
      <button @click="verFacturasPagadas">Ver facturas pagadas</button>
      <p v-if="consultaEstado">{{ consultaEstado }}</p>
      <ul><li v-for="(factura, index) in busquedaFacturas" :key="index"><pre>{{ factura }}</pre></li></ul>
    </section>

    <section v-if="pagina === 'F10'">
      <h2>Orden de accidentalidad</h2>
      <p>Los accidentes con mayor dano aparecen primero.</p>
      <p v-if="colaAccidentes.length === 0">No hay accidentes esperando.</p>
      <ol><li v-for="item in colaAccidentes" :key="item.turno">Turno #{{ item.turno }} | {{ item.placa }} | Dano {{ item.dano }} | {{ item.cliente }} | {{ formatoDinero(item.valor) }}</li></ol>
      <button @click="atenderSiguiente('ACCIDENTE')">Atender siguiente accidente</button>
      <h3>Cola de mantenimiento</h3>
      <ol><li v-for="item in colaMantenimiento" :key="item.turno">Turno #{{ item.turno }} | {{ item.placa }} | {{ item.cliente }}</li></ol>
      <button @click="atenderSiguiente('MANTENIMIENTO')">Atender siguiente mantenimiento</button>
    </section>
  </main>
</template>
