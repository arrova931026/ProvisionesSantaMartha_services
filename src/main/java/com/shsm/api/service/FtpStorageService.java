package com.shsm.api.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

/**
 * Servicio de almacenamiento de archivos sobre FTP.
 *
 * <p>Estructura en el servidor FTP:</p>
 * <pre>
 *   mutualista/
 *     profile_pictures/{CURP}.{ext}
 *     docs/{personaId}/{TIPO}.{ext}
 *     docs/{personaId}/VIDEO_CONSENT.{ext}
 * </pre>
 *
 * <p><b>Nota de seguridad:</b> FTP (puerto 21) transmite credenciales y datos
 * en texto plano. Migrá a SFTP o FTPS en cuanto sea posible.</p>
 */
@Slf4j
@Service
public class FtpStorageService {

    @Value("${ftp.host}")
    private String host;

    @Value("${ftp.port:21}")
    private int port;

    @Value("${ftp.username}")
    private String username;

    @Value("${ftp.password}")
    private String password;

    /** Directorio raíz dentro del servidor FTP (ej: "mutualista"). */
    @Value("${ftp.remote-base-dir:mutualista}")
    private String remoteBaseDir;

    /**
     * URL base pública HTTP desde donde se sirven los archivos
     * (ej: "https://sociedadhumanistasantamartha.com/mutualista").
     */
    @Value("${ftp.public-base-url}")
    private String publicBaseUrl;

    // ── API pública ──────────────────────────────────────────────────────────

    /**
     * Sube un archivo al servidor FTP.
     *
     * @param inputStream contenido del archivo (se cierra externamente)
     * @param remotePath  ruta relativa al directorio base,
     *                    ej: "profile_pictures/CURP.jpg"
     * @return URL pública accesible por HTTP
     * @throws IOException si la conexión o la transferencia fallan
     */
    public String upload(InputStream inputStream, String remotePath) throws IOException {
        FTPClient ftp = connect();
        try {
            String fullPath = remoteBaseDir + "/" + remotePath;
            ensureDirectories(ftp, fullPath);
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
            boolean ok = ftp.storeFile(fullPath, inputStream);
            if (!ok) {
                throw new IOException(
                    "FTP storeFile falló para: " + fullPath + " | " + ftp.getReplyString().trim());
            }
            log.info("FTP upload OK: {}", fullPath);
            return publicBaseUrl + "/" + remotePath;
        } finally {
            disconnect(ftp);
        }
    }

    /**
     * Elimina un archivo del servidor FTP.
     * No lanza excepción si el archivo no existe o la conexión falla
     * (registra WARN en el log).
     *
     * @param remotePath ruta relativa al directorio base
     */
    public void delete(String remotePath) {
        if (remotePath == null || remotePath.isBlank()) return;
        try {
            FTPClient ftp = connect();
            try {
                String fullPath = remoteBaseDir + "/" + remotePath;
                boolean ok = ftp.deleteFile(fullPath);
                if (ok) {
                    log.info("FTP delete OK: {}", fullPath);
                } else {
                    log.warn("FTP delete: archivo no encontrado o no eliminado: {}", fullPath);
                }
            } finally {
                disconnect(ftp);
            }
        } catch (IOException e) {
            log.warn("FTP delete falló para {}: {}", remotePath, e.getMessage());
        }
    }

    /**
     * Extrae la ruta relativa a partir de una URL pública generada por este servicio.
     * Útil para pasar a {@link #delete(String)}.
     *
     * @param publicUrl URL pública completa
     * @return ruta relativa, o {@code null} si la URL no corresponde al servidor
     */
    public String toRemotePath(String publicUrl) {
        if (publicUrl == null || !publicUrl.startsWith(publicBaseUrl)) return null;
        return publicUrl.substring(publicBaseUrl.length()).replaceFirst("^/+", "");
    }

    // ── Helpers privados ─────────────────────────────────────────────────────

    private FTPClient connect() throws IOException {
        FTPClient ftp = new FTPClient();
        ftp.setConnectTimeout(10_000);
        ftp.setDefaultTimeout(15_000);
        ftp.connect(host, port);
        if (!ftp.login(username, password)) {
            ftp.disconnect();
            throw new IOException("FTP login falló para el usuario: " + username);
        }
        ftp.enterLocalPassiveMode();   // evita problemas con NAT/firewall
        ftp.setFileType(FTP.BINARY_FILE_TYPE);
        return ftp;
    }

    private void disconnect(FTPClient ftp) {
        try {
            if (ftp.isConnected()) {
                ftp.logout();
                ftp.disconnect();
            }
        } catch (IOException e) {
            log.warn("Error al desconectar FTP: {}", e.getMessage());
        }
    }

    /**
     * Crea los directorios intermedios de la ruta si no existen.
     * Ignora errores (el servidor puede rechazar {@code MKD} si ya existe).
     */
    private void ensureDirectories(FTPClient ftp, String filePath) {
        String[] parts = filePath.split("/");
        StringBuilder current = new StringBuilder();
        // Los directorios son todos los segmentos excepto el último (nombre de archivo)
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].isBlank()) continue;
            current.append("/").append(parts[i]);
            try {
                ftp.makeDirectory(current.toString());
            } catch (IOException e) {
                // El directorio puede ya existir — no es un error fatal
            }
        }
    }
}
