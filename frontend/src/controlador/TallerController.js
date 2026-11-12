// Controlador: valida los formularios y llama al backend Java.
import { computed, reactive, ref } from 'vue'
import { api, enviar } from '../modelo/Taller.js'

export function crearControladorTaller() {
  const pagina = ref('F1')
  const mensaje = ref('Conectando con el servidor del taller...')
  const datos = reactive({ mecanico: null, clienteActivo: null, automovilActivo: null, reparacionActiva: null, inventario: [], clientes: [], automoviles: [], facturas: [], ultimaFactura: null, ultimoPago: null })
  const acceso = reactive({ nombre: '', contrasena: '' })
  const clienteForm = reactive({ id: '', nombre: '', telefono: '', ingresos: '', pedirCredito: false })
  const autoForm = reactive({ placa: '', marca: '', modelo: '', anio: '', tipo: 'Gasolina', cilindraje: 'C4', autonomia: '' })
  const reparacionForm = reactive({ tipo: 'MANTENIMIENTO', dano: 1, valor: '', repuestos: {}, credito: false })
  const pagoForm = reactive({ tipoPago: 'actual', medio: 'Efectivo', recibido: '', cuotas: 1, abono: '' })
  const buscarId = ref('')
  const consultaPlaca = ref('')
  const consultaEstado = ref('')
  const busquedaFacturas = ref([])
  const colaMantenimiento = ref([])
  const colaAccidentes = ref([])

  const repuestosElegidos = computed(() => {
    const nombres = []
    for (const [nombre, cantidad] of Object.entries(reparacionForm.repuestos))
      for (let i = 0; i < Number(cantidad || 0); i++) nombres.push(nombre)
    return nombres
  })

  function avisar(texto) { mensaje.value = texto }
  function convertirError(error) { avisar(error.message || 'No se pudo conectar con el servidor.') }

  async function actualizarColas() {
    const colas = await api('/queues')
    colaMantenimiento.value = colas.mantenimiento
    colaAccidentes.value = colas.accidentes
  }

  async function cargarSesion() {
    try {
      const [sesion, inventario] = await Promise.all([api('/session'), api('/inventory')])
      datos.mecanico = sesion.mecanico
      datos.clienteActivo = sesion.cliente
      datos.automovilActivo = sesion.automovil
      datos.reparacionActiva = sesion.reparacion
      datos.inventario = inventario
      await actualizarColas()
      avisar('Conectado con el servidor Java.')
    } catch (error) { convertirError(error) }
  }
  cargarSesion()

  // F1. MongoDB valida las credenciales del mecánico.
  async function autenticar() {
    try {
      datos.mecanico = await enviar('/auth', acceso)
      avisar(`Mecánico autenticado: ${datos.mecanico.nombre}.`)
      pagina.value = 'F2'
    } catch (error) { convertirError(error) }
  }

  // F2. Registrar o buscar cliente.
  async function registrarCliente() {
    try {
      datos.clienteActivo = await enviar('/clients', {
        id: clienteForm.id, nombre: clienteForm.nombre, telefono: clienteForm.telefono,
        ingresos: Number(clienteForm.ingresos || 0), pedirCredito: clienteForm.pedirCredito,
      })
      datos.clientes.push(datos.clienteActivo)
      Object.assign(clienteForm, { id: '', nombre: '', telefono: '', ingresos: '', pedirCredito: false })
      avisar(`Cliente registrado: ${datos.clienteActivo.nombre}.`)
      pagina.value = 'F3'
    } catch (error) { convertirError(error) }
  }

  async function buscarCliente() {
    try {
      datos.clienteActivo = await api('/clients/' + encodeURIComponent(buscarId.value.trim()))
      datos.clientes.push(datos.clienteActivo)
      avisar(`Cliente encontrado: ${datos.clienteActivo.nombre}.`)
    } catch (error) { convertirError(error) }
  }

  // F3. Registrar automóvil para el cliente activo.
  async function registrarAutomovil() {
    try {
      datos.automovilActivo = await enviar('/cars', {
        placa: autoForm.placa.toUpperCase(), marca: autoForm.marca, modelo: autoForm.modelo,
        anio: Number(autoForm.anio), tipo: autoForm.tipo, cilindraje: autoForm.cilindraje,
        autonomia: Number(autoForm.autonomia || 0),
      })
      datos.automoviles.push(datos.automovilActivo)
      Object.assign(autoForm, { placa: '', marca: '', modelo: '', anio: '', autonomia: '' })
      avisar(`Automóvil ${datos.automovilActivo.placa} registrado.`)
      pagina.value = 'F4'
    } catch (error) { convertirError(error) }
  }

  // F4. El backend carga la bolsa desde data/Repuestos.txt.
  async function cargarRepuestos() {
    try {
      datos.inventario = await enviar('/inventory/load', {})
      avisar(`Repuestos cargados: ${datos.inventario.length} referencias.`)
    } catch (error) { convertirError(error) }
  }

  // F5. El servicio Java comprueba existencias y asigna la reparación a una cola.
  async function crearReparacion() {
    try {
      datos.reparacionActiva = await enviar('/repairs', {
        tipo: reparacionForm.tipo, dano: Number(reparacionForm.dano), valor: Number(reparacionForm.valor),
        repuestos: repuestosElegidos.value, credito: reparacionForm.credito,
      })
      // El servidor ya descontó los repuestos; volvemos a consultar las cantidades.
      datos.inventario = await api('/inventory')
      datos.ultimoPago = null
      Object.assign(reparacionForm, { tipo: 'MANTENIMIENTO', dano: 1, valor: '', repuestos: {}, credito: false })
      await actualizarColas()
      avisar(`Reparación creada. Turno #${datos.reparacionActiva.turno}.`)
      pagina.value = 'F6'
    } catch (error) { convertirError(error) }
  }

  // F6. El backend ejecuta la estrategia Java para el tipo de automóvil.
  async function ejecutarPuestaAPunto() {
    try {
      const resumen = await enviar('/maintenance', {})
      datos.reparacionActiva.resumenMantenimiento = resumen
      avisar('Puesta a punto terminada.')
    } catch (error) { convertirError(error) }
  }

  // F7. El backend procesa PagarReparacion/PagarSaldoPendiente y el medio elegido.
  async function procesarPago() {
    try {
      datos.ultimoPago = await enviar('/payments', {
        destino: pagoForm.tipoPago, medio: pagoForm.medio,
        abono: Number(pagoForm.abono || 0), recibido: Number(pagoForm.recibido || 0), cuotas: Number(pagoForm.cuotas || 1),
      })
      avisar(`Pago procesado: ${formatoDinero(datos.ultimoPago.monto)}.`)
      pagina.value = 'F8'
    } catch (error) { convertirError(error) }
  }

  // F8. La factura se guarda en MongoDB y el servicio registra la salida.
  async function generarFactura() {
    try {
      datos.ultimaFactura = await enviar('/invoices', {})
      datos.facturas.push(datos.ultimaFactura)
      datos.reparacionActiva = null
      datos.automovilActivo = null
      datos.clienteActivo = null
      datos.ultimoPago = null
      await actualizarColas()
      avisar('Factura guardada en MongoDB. Salida autorizada.')
    } catch (error) { convertirError(error) }
  }

  // F9. Estas consultas leen MongoDB, igual que Main.
  async function consultarEstado() {
    try { consultaEstado.value = await api('/invoices/status/' + encodeURIComponent(consultaPlaca.value.trim().toUpperCase())) }
    catch (error) { convertirError(error) }
  }
  async function buscarFacturasPorPlaca() {
    try { busquedaFacturas.value = await api('/invoices/plate/' + encodeURIComponent(consultaPlaca.value.trim().toUpperCase())) }
    catch (error) { convertirError(error) }
  }
  async function verFacturasPagadas() {
    try { busquedaFacturas.value = await api('/invoices/paid') }
    catch (error) { convertirError(error) }
  }

  // F10 y F11. La cola de prioridad se consulta y atiende en el backend.
  async function atenderSiguiente(tipo) {
    try {
      datos.reparacionActiva = await enviar('/queues/attend/' + tipo, {})
      datos.clienteActivo = { id: datos.reparacionActiva.clienteId, nombre: datos.reparacionActiva.cliente, saldo: 0, credito: datos.reparacionActiva.credito }
      datos.automovilActivo = { placa: datos.reparacionActiva.placa, tipo: datos.reparacionActiva.tipoAuto }
      await actualizarColas()
      pagina.value = 'F6'
      avisar(`Atendiendo turno #${datos.reparacionActiva.turno}.`)
    } catch (error) { convertirError(error) }
  }

  function seleccionarCliente(cliente) { datos.clienteActivo = cliente }
  function seleccionarAutomovil(auto) { datos.automovilActivo = auto }
  function autosDelCliente() { return datos.automoviles.filter(auto => auto.clienteId === datos.clienteActivo?.id) }
  function formatoDinero(valor) { return '$' + Number(valor || 0).toLocaleString('es-CO') }

  return {
    pagina, mensaje, datos, acceso, clienteForm, buscarId, autoForm, reparacionForm, pagoForm,
    consultaPlaca, consultaEstado, busquedaFacturas, colaMantenimiento, colaAccidentes,
    repuestosElegidos, autenticar, registrarCliente, buscarCliente, registrarAutomovil,
    cargarRepuestos, crearReparacion, ejecutarPuestaAPunto, procesarPago, generarFactura,
    consultarEstado, buscarFacturasPorPlaca, verFacturasPagadas, atenderSiguiente,
    seleccionarCliente, seleccionarAutomovil, autosDelCliente, formatoDinero, actualizarColas,
  }
}
