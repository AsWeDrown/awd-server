package gg.aswedrown.game.entity;

public abstract class LivingEntity extends Entity {

    public float maxHealth     = 0.0f, // максимальный запас здоровья этой сущности (в хитпоинтах) (не должен меняться)
                 currentHealth = 0.0f; // текущий запас здоровья этой сущности (в хитпоинтах)

    public LivingEntity(int entityType) {
        super(entityType);
    }

    public void kill() {
        currentHealth = 0.0f;
    }

}
