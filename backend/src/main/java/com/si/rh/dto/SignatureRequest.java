//Brandon
package com.si.rh.dto;

public class SignatureRequest {
    private String partie;
    private String signatureBase64;
    public String getPartie() { return partie; }
    public void setPartie(String partie) { this.partie = partie; }


    public String getSignatureBase64() { return signatureBase64; }
public void setSignatureBase64(String signatureBase64) { this.signatureBase64 = signatureBase64; }
}