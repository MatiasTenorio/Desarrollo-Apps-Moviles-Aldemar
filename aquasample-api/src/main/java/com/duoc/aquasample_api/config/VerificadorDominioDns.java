package com.duoc.aquasample_api.config;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import org.springframework.stereotype.Component;

/**
 * Pregunta al DNS por los registros MX del dominio (los servidores que reciben
 * su correo), con el cliente DNS que trae Java (JNDI), sin librerías extra.
 * Sin MX se acepta un registro A (regla de SMTP: el propio servidor recibe).
 * Un MX "." (RFC 7505) significa que el dominio declara no recibir correo.
 */
@Component
public class VerificadorDominioDns implements VerificadorDominio {

    @Override
    public EstadoDominio verificar(String dominio) {
        Hashtable<String, String> entorno = new Hashtable<>();
        entorno.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        entorno.put("com.sun.jndi.dns.timeout.initial", "2000");
        entorno.put("com.sun.jndi.dns.timeout.retries", "1");
        try {
            DirContext dns = new InitialDirContext(entorno);
            try {
                Attribute mx = dns.getAttributes(dominio, new String[] {"MX"}).get("MX");
                if (mx != null && mx.size() > 0) {
                    return soloMxNulo(mx) ? EstadoDominio.SIN_CORREO : EstadoDominio.RECIBE_CORREO;
                }
                Attribute a = dns.getAttributes(dominio, new String[] {"A"}).get("A");
                return a != null && a.size() > 0 ? EstadoDominio.RECIBE_CORREO : EstadoDominio.SIN_CORREO;
            } finally {
                dns.close();
            }
        } catch (NameNotFoundException e) {
            return EstadoDominio.NO_EXISTE;
        } catch (NamingException e) {
            return EstadoDominio.NO_SE_PUDO_VERIFICAR;
        }
    }

    private static boolean soloMxNulo(Attribute mx) throws NamingException {
        NamingEnumeration<?> valores = mx.getAll();
        while (valores.hasMore()) {
            String valor = valores.next().toString().trim();
            if (!valor.endsWith(" .") && !valor.equals(".")) {
                return false;
            }
        }
        return true;
    }
}
