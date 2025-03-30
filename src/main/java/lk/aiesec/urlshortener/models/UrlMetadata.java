package lk.aiesec.urlshortener.models;


import lombok.Data;

@Data
public class UrlMetadata {
    private String targetUrl;
    private String title;
    private String description;
    private String imageUrl;
    private String type;

    public UrlMetadata(String targetUrl, String title, String description, String imageUrl, String type) {
        this.targetUrl = (targetUrl == null || targetUrl.isEmpty()) ? "https://signup.aiesec.lk/" : targetUrl;
        this.title = (title == null || title.isEmpty()) ? "AIESEC Sri-Lanka" : title;
        this.description = (description == null || description.isEmpty()) ? "Peace and fulfillment of humankind's potential" : description;
        this.imageUrl = (imageUrl == null || imageUrl.isEmpty()) ? "https://aiesec-logos.s3.eu-west-1.amazonaws.com/AIESEC-Human-White.png" : imageUrl;
        this.type = (type == null || type.isEmpty()) ? "website" : type;
    }

}
