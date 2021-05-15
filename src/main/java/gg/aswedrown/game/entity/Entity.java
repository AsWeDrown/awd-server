package gg.aswedrown.game.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.Objects;

public abstract class Entity {

    @Getter
    private final int entityType; // тип сущности

    @Getter @Setter
    protected int currentDimension; // текущий мир (измерение), в котором находится сущность

    @Getter @Setter
    protected int entityId; // ID этой сущности в её текущем мире

    @Getter
    protected float posX         = 0.0f, // координата X в мире
                    posY         = 0.0f, // координата Y в мире
                    faceAngle    = 0.0f, // угол, указывающий, в какую сторону сейчас смотрит эта сущность
                    spriteWidth  = 0.0f, // ширина спрайта (модельки)
                    spriteHeight = 0.0f; // высота спрайта (модельки)

    public Entity(int entityType) {
        this.entityType = entityType;
    }

    /**
     * Выполняется каждый тик, служит для обновления данных об этой сущности (game state update).
     */
    public abstract void update();

    /**
     * Генерирует контейнер вида "ключ-значение", содержащий различные важные параметры об этой сущности,
     * которые должны учитываться всеми игроками (эти данные передаются по сети). Ключами служат названия
     * передаваемых параметров (например, "color"), а значениями - собственно, значения передаваемых параметров
     * (например, "0xffffff"). Все значения должны быть преобразованы к строке, независимо от их реального типа.
     * Это нужно для универсальности. Клиент сам преобразует значения из строк в нужные типы, если потребуется.
     */
    public abstract Map<String, String> formEntityData();

    public void setPosition(float posX, float posY) {
        this.posX = posX;
        this.posY = posY;
    }

    public void move(float deltaX, float deltaY) {
        setPosition(posX + deltaX, posY + deltaY);
    }

    public void setRotation(float faceAngle) {
        this.faceAngle = faceAngle;
    }

    public void rotate(float deltaFaceAngle) {
        setRotation(faceAngle + deltaFaceAngle);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Entity)) return false;
        Entity entity = (Entity) o;
        return entityId == entity.entityId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityId);
    }

}
