// El modelo pide o envía información al backend Java.
export async function api(ruta, opciones = {}) {
  const configuracion = {
    ...opciones,
    headers: { 'Content-Type': 'application/json', ...opciones.headers },
  }
  const respuesta = await fetch('/api' + ruta, configuracion)
  const texto = await respuesta.text()
  let resultado = null
  if (texto) {
    try { resultado = JSON.parse(texto) }
    catch { resultado = texto }
  }
  if (!respuesta.ok) throw new Error(resultado?.error || resultado || 'Error del servidor.')
  return resultado
}

export function enviar(ruta, datos) {
  return api(ruta, { method: 'POST', body: JSON.stringify(datos) })
}
