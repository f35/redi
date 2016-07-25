/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.marmotta.ucuenca.wk.commons.impl;

import org.slf4j.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import org.apache.marmotta.ucuenca.wk.commons.service.CommonsServices;

/**
 *
 * @author FernandoBac
 */
public class CommonsServicesImpl implements CommonsServices {

    @Inject
    private Logger log;
    
    /**
     * Función que elimina acentos y caracteres especiales
     *
     * @param value
     * @return cadena de texto limpia de acentos y caracteres especiales.
     */
    @Override
    public String removeAccents(String input) {
        // Cadena de caracteres original a sustituir.
        String original = "áàäéèëíìïóòöúùuñÁÀÄÉÈËÍÌÏÓÒÖÚÙÜÑçÇ";
        // Cadena de caracteres ASCII que reemplazarán los originales.
        String ascii = "aaaeeeiiiooouuunAAAEEEIIIOOOUUUNcC";
        String output = input;
        for (int i = 0; i < original.length(); i++) {
            // Reemplazamos los caracteres especiales.
            output = output.replace(original.charAt(i), ascii.charAt(i));
        }//for i
        return output;
    }

    /**
     * Return true or false if object is a URI
     */
    @Override
    public Boolean isURI(String object) {
        URL url = null;
        try {
            url = new URL(object);
        } catch (Exception e1) {
            return false;
        }
        Pattern pat = Pattern.compile("^[hH]ttp(s?)");
        Matcher mat = pat.matcher(url.getProtocol());
        return mat.matches(); // return "http".equals(url.getProtocol()) || "https".equals(url.getProtocol()) ;
    }

    @Override
    public String getMD5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger number = new BigInteger(1, messageDigest);
            String hashtext = number.toString(16);
            // Now we need to zero pad it if you actually want the full 32 chars.
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getReadPropertyFromFile(String file,String property) {
        Properties propiedades = new Properties();
        InputStream entrada = null;
        ConcurrentHashMap<String, String> mapping = new ConcurrentHashMap<String, String>();
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            entrada = classLoader.getResourceAsStream(file);
            propiedades.load(entrada);
            for (String source : propiedades.stringPropertyNames()) {
                String target = propiedades.getProperty(source);
                mapping.put(source, target);

            }
        } catch (IOException ex) {
            log.error("IOException in getReadPropertyFromFile CommonsServiceImpl " + ex);
        } catch (Exception ex) {
            log.error("Exception in getReadPropertyFromFile CommonsServiceImpl " + ex);
        } finally {
            if (entrada != null) {
                try {
                    entrada.close();
                } catch (IOException e) {
                    log.error("IOException un getReadPropertyFromFile line 106" + e);
                }
            }
        }
        return mapping.get(property);
    }
}
