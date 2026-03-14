package Model;

public class Card {
    private int cardId, lotId;
    private String cardCode, cardType;
    private boolean status;

    public Card(int cardId, int lotId, String cardCode, String cardType, boolean status) {
        this.cardId = cardId;
        this.lotId = lotId;
        this.cardCode = cardCode;
        this.cardType = cardType;
        this.status = status;
    }

    public Card() {
    }

    public int getCardId() {
        return cardId;
    }

    public void setCardId(int cardId) {
        this.cardId = cardId;
    }

    public int getLotId() {
        return lotId;
    }

    public void setLotId(int lotId) {
        this.lotId = lotId;
    }

    public String getCardCode() {
        return cardCode;
    }

    public void setCardCode(String cardCode) {
        this.cardCode = cardCode;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }
}
