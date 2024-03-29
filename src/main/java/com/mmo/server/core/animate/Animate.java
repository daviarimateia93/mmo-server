package com.mmo.server.core.animate;

import static java.lang.Math.*;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mmo.server.core.attribute.Attribute;
import com.mmo.server.core.attribute.AttributeModifier;
import com.mmo.server.core.attribute.Attributes;
import com.mmo.server.core.game.Game;
import com.mmo.server.core.looper.LooperContext;
import com.mmo.server.core.map.Map;
import com.mmo.server.core.map.MapEntity;
import com.mmo.server.core.map.Position;
import com.mmo.server.core.math.Vertex;
import com.mmo.server.core.packet.Packet;
import com.mmo.server.core.property.PropertyModifierAction;

public abstract class Animate implements MapEntity {

    private static final int MOVE_UPDATE_RATIO_IN_MILLIS = 300;

    private static final Logger logger = LoggerFactory.getLogger(Animate.class);

    private Long lastAttackStartTime;
    private Long lastMoveStartTime;
    private Animate targetAnimate;
    private Position targetPosition;
    private boolean collided;
    private float moveDistanceRemainderX = 0;
    private float moveDistanceRemainderZ = 0;

    public abstract UUID getId();

    public abstract Attributes getAttributes();

    public boolean isAttacking() {
        return Objects.nonNull(lastAttackStartTime);
    }

    public boolean isMoving() {
        return Objects.nonNull(lastMoveStartTime) && !hasCollided();
    }

    public boolean isAlive() {
        return getAttributes().getFinalHP() > 0;
    }

    public boolean isInsideAttackRange(Position position) {
        int attackRange = getAttributes().getFinalAttackRange();

        return getPosition().isNearby(position, attackRange);
    }

    public boolean hasCollided() {
        return collided;
    }

    public Optional<Position> getTargetPosition() {
        Position position = getTargetAnimate()
                .map(Animate::getPosition)
                .orElse(targetPosition);

        return Optional.ofNullable(position);
    }

    public Optional<Animate> getTargetAnimate() {
        return Optional.ofNullable(targetAnimate);
    }

    public Optional<Long> getLastAttackStartTime() {
        return Optional.ofNullable(lastAttackStartTime);
    }

    public Optional<Long> getLastMoveStartTime() {
        return Optional.ofNullable(lastMoveStartTime);
    }

    /*
     * (attackSpeed / 100) by second
     * (attackSpeed / 100 / 1000) by millis
     * ---------------------------------
     * examples:
     * 025 -> 4s
     * 050 -> 2s
     * 100 -> 1s
     * 200 -> 0.5s
     * 400 -> 0.25s
     * attackSpeed * desiredSeconds = 100
     * desiredSeconds = 100/attackSpeed
     */
    public int getAttackUpdateRateInMillis() {
        return (100 / getAttributes().getFinalAttackSpeed()) * 1000;
    }

    public int getMoveUpdateRateInMillis() {
        return MOVE_UPDATE_RATIO_IN_MILLIS;
    }

    public void attack(Animate target) {
        clearTargetPosition();
        targetAnimate = target;
        lastAttackStartTime = getNewTick();
        onBeginAttack();
    }

    private void attack() {
        if (!isAttacking()) {
            return;
        }

        Animate target = getTargetAnimate().orElseThrow();
        int attack = getAttributes().getFinalAttack();
        int targetDefense = target.getAttributes().getFinalDefense();
        int damage = attack - targetDefense;

        target.getAttributes().modify(AttributeModifier.builder()
                .action(PropertyModifierAction.DECREMENT)
                .attribute(Attribute.HP)
                .value(damage)
                .build());

        target.onDamage(damage, this);

        onAttack(damage);

        if (!target.isAlive() && Objects.nonNull(lastAttackStartTime)) {
            stopAttacking();
            target.onDie(this);
        } else {
            lastAttackStartTime = getNewTick();
        }
    }

    public void move(Position target) {
        clearTargetAnimate();
        collided = false;
        targetPosition = target;
        lastMoveStartTime = getNewTick();
        onBeginMove();
    }

    private void move(Long lastMoveStartTime) {
        collided = false;
        this.lastMoveStartTime = lastMoveStartTime;
        onBeginMove();
    }

    private void move() {
        if (!isMoving()) {
            return;
        }

        Position current = getPosition();
        Position target = getTargetPosition().orElseThrow();

        Vertex distance = getMoveDistance(current, target);
        int distanceX = moveX(current, target, distance.getX());
        int distanceZ = moveZ(current, target, distance.getZ());

        onMove(distanceX, distanceZ);

        if (hasFinishedMoving(current, target) && Objects.nonNull(lastMoveStartTime)) {
            stopMoving();
        } else {
            lastMoveStartTime = getNewTick();
        }
    }

    public Vertex getMoveDistance(Position current, Position target) {
        int finalMoveSpeed = getAttributes().getFinalMoveSpeed();

        float targetOtherPointDistance = abs(target.getX() - current.getX());
        float playerOtherPointDistance = abs(current.getZ() - target.getZ());

        float playerTargetAngle = (float) toDegrees(atan(targetOtherPointDistance / playerOtherPointDistance));

        float distanceX = (playerTargetAngle * finalMoveSpeed) / 90;
        float distanceZ = finalMoveSpeed - distanceX;

        logger.trace("Animate {} calculated move distance ({}, {})", getInstanceId(), distanceX, distanceZ);

        float remainderX = distanceX % 1;
        float remainderZ = distanceZ % 1;

        int distanceXAsInt = (int) (distanceX - remainderX);
        int distanceZAsInt = (int) (distanceZ - remainderZ);

        moveDistanceRemainderX += remainderX;
        moveDistanceRemainderZ += remainderZ;

        if (moveDistanceRemainderX >= 1) {
            distanceXAsInt++;
            moveDistanceRemainderX -= 1;
        }

        if (moveDistanceRemainderZ >= 1) {
            distanceZAsInt++;
            moveDistanceRemainderZ -= 1;
        }

        return new Vertex(distanceXAsInt, distanceZAsInt);
    }

    private int moveX(Position current, Position target, int distance) {
        int distanceX = 0;

        if (current.getX() < target.getX()) {
            int difference = target.getX() - current.getX();
            distanceX = distance > difference ? difference : distance;
            collided = !current.incrementX(distanceX);
        } else if (current.getX() > target.getX()) {
            int difference = current.getX() - target.getX();
            distanceX = distance > difference ? difference : distance;
            collided = !current.decrementX(distanceX);
        }

        return distanceX;
    }

    private int moveZ(Position current, Position target, int distance) {
        int distanceZ = 0;

        if (current.getZ() < target.getZ()) {
            int difference = target.getZ() - current.getZ();
            distanceZ = distance > difference ? difference : distance;
            collided = !current.incrementZ(distanceZ);
        } else if (current.getZ() > target.getZ()) {
            int difference = current.getZ() - target.getZ();
            distanceZ = distance > difference ? difference : distance;
            collided = !current.decrementZ(distanceZ);
        }

        return distanceZ;
    }

    private boolean hasFinishedMoving(Position current, Position target) {
        return current.equals(target) || hasCollided();
    }

    @Override
    public void update(LooperContext context) {
        if (isAttacking()) {
            if (isInsideAttackRange(targetAnimate.getPosition())) {
                // we reach our target
                stopMoving();
            } else {
                // we should start moving to getting closer
                move(lastAttackStartTime);
            }

            update(context, lastAttackStartTime, getAttackUpdateRateInMillis(), this::attack);
        }

        if (isMoving()) {
            update(context, lastMoveStartTime, getMoveUpdateRateInMillis(), this::move);
        }
    }

    private void update(LooperContext context, long startTime, int updateRate, Runnable runnable) {
        long difference = context.getTick() - startTime;

        while (difference >= updateRate) {
            runnable.run();
            difference -= updateRate;
        }
    }

    private void clearTargetPosition() {
        targetPosition = null;
    }

    private void clearTargetAnimate() {
        targetAnimate = null;
    }

    public void stopMoving() {
        lastMoveStartTime = null;
        onFinishMove();
    }

    public void stopAttacking() {
        lastAttackStartTime = null;
        onFinishAttack();
    }

    protected void dispatch(Packet packet) {
        getMap().dispatch(packet);
    }

    protected void dispatch(Packet packet, UUID target) {
        getMap().dispatch(packet, target);
    }

    protected Map getMap() {
        return Game.getInstance().getMap();
    }

    protected void onBeginMove() {
        logger.info("Animate {} has began moving to {}",
                getInstanceId(),
                getTargetPosition().orElseThrow());
    }

    protected void onFinishMove() {
        logger.info("Animate {} has finished moving to {}",
                getInstanceId(),
                getTargetPosition().orElseThrow());
    }

    protected void onMove(int distanceX, int distanceZ) {
        logger.trace("Animate {} has moved to {}",
                getInstanceId(),
                getPosition());
    }

    protected void onBeginAttack() {
        logger.info("Animate {} has began attacking {}",
                getInstanceId(),
                getTargetAnimate().orElseThrow().getInstanceId());
    }

    protected void onFinishAttack() {
        logger.info("Animate {} has finished attacking {}",
                getInstanceId(),
                getTargetAnimate().orElseThrow().getInstanceId());
    }

    protected void onDamage(int damage, Animate source) {
        logger.trace("Animate {} has suffered damage of {} by {}",
                getInstanceId(),
                damage,
                source.getInstanceId());
    }

    protected void onAttack(int damage) {
        logger.trace("Animate {} has attacked {} with damage of {}",
                getInstanceId(),
                getTargetAnimate().orElseThrow().getInstanceId(),
                damage);
    }

    protected void onDie(Animate source) {
        logger.info("Animate {} has died by {}",
                getInstanceId(),
                source.getInstanceId());
    }

    private static long getNewTick() {
        return System.currentTimeMillis();
    }
}
