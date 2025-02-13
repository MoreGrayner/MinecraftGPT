package com.moregrayner.plugins.eventGPTS;


import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.components.FoodComponent;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.bukkit.Bukkit.getLogger;

public class EventX implements Listener { //Boolean이든 String이든 통일할 필요가 있을 듯

    private static EventX instance;
    private GPT gpt;

    //final은 재할당이 금지되지만 내부 값 수정은 가능해서 final로 선언함.
    private final HashMap<UUID, Boolean> Move = new HashMap<>();

    private final HashMap<UUID, String> broken = new HashMap<>();
    private final HashMap<UUID, String> placen = new HashMap<>();

    //이벤트 처리중 귀찮음 이슈로 UUID 포기(수정하려면 한 줄만 추가하면 됨)
    private final HashMap<String, String> attacked = new HashMap<>();
    private final HashMap<String, String> damaged = new HashMap<>();

    private final HashMap<String, String> damages = new HashMap<>();

    private final HashMap<UUID, Boolean> playerDeathStatus = new HashMap<>();
    private final HashMap<UUID, Boolean> isKilledStatus = new HashMap<>();

    private final Map<UUID, Map<InventoryType, Boolean>> InV = new HashMap<>();
    private final Map<UUID, Map<InventoryType, Boolean>> InVC = new HashMap<>();

    private final HashMap<UUID, Boolean> ItemSs = new HashMap<>();



    public EventX(GPT gpt){
        this.gpt = gpt;
    }



    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){//플레이어 입탈주 관리
        Player player = event.getPlayer();
        String guide = "플레이어가 서버에 들어왔는데 이에 맞은 가이드라인을 한 줄로 요약해서 보내줘";
        String message = gpt.getGuide(guide);
        player.sendMessage(message);
        player.sendMessage(ChatColor.YELLOW + "해당 플러그인은 야생용으로 제작된 프로토타입이므로 야생 외의 메커니즘은 지원하지 않습니다. 이용에 참고해 주세요.");
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        //null
    }
    @EventHandler
    public void onPlayerMoVe(PlayerMoveEvent event){
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        boolean hasMoved = Move.getOrDefault(uuid, false);
        if (!hasMoved) {
            return;
        }
        String guide = "플레이어가 처음 움직였어! 덕담을 한 줄로 요약해서 보내 줘";
        String message = gpt.getGuide(guide);
        player.sendMessage(message);
        Move.put(uuid, true);
    }
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event){//블록 가져오고 null 아닌거 처리
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Block breaked = event.getBlock();

        if (broken.containsKey(uuid)) {
            return;
        }
        String guide = "이 블록의 정보를 한줄로 요약해서 보내줘" + breaked ;
        String message = gpt.getGuide(guide);
        player.sendMessage(message);
        broken.put(uuid, breaked.getType().toString());

    }
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event){
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Block placed = event.getBlock();

        if (placen.containsKey(uuid)||broken.containsKey(uuid)) {
            return;
        }
        String guide = "이 블록의 정보를 한줄로 요약해서 보내줘" + placed ;
        String message = gpt.getGuide(guide);
        player.sendMessage(message);
        placen.put(uuid, placed.getType().toString());
    }
    @EventHandler
    public void onDamageE(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity(); //공격받은 엔티티
        Entity damage = event.getDamager(); //요녀석이 공격한 엔티티임

        if (damage instanceof Player player) {//플레이어가 공격했을 때
            if (!attacked.containsValue(entity.getType().toString())) { //때린 엔티티 다루기
                String guide = "이 몹의 정보와 주의사항을 한 줄로 요약해서 보내줘" + entity;
                String message = gpt.getGuide(guide);
                player.sendMessage(message);
                attacked.put(player.getName(), entity.getType().toString());
            }
        }else if (entity instanceof Player player) {
            if (!damaged.containsValue(damage.getType().toString())) {//여기가 공격한 엔티티 다루는부분
                String guide = "이 몹의 정보와 주의사항을 한 줄로 요약해서 보내줘" + damage;
                String message = gpt.getGuide(guide);
                player.sendMessage(message);
                damaged.put(player.getName(), entity.getType().toString());
            }
        }
    }
    @EventHandler
    public void onDamage(EntityDamageEvent event){
        Entity entity = event.getEntity();
        EntityDamageEvent.DamageCause damageCause = event.getCause();
        if (entity instanceof Player player){
            if (!damages.containsValue(damageCause.toString().toLowerCase())) {
                String guide = event.getCause().toString().toLowerCase() + "이것에 대해 한 줄로 정리해서 조언을 해 줘";
                String message = gpt.getGuide(guide);
                player.sendMessage(message);
                damages.put(player.getName(), entity.getType().toString());
            }
        }
    }
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event){
        Player player = event.getEntity(); //왜 엔티티임? - 엔티티 죽었을때 메시지가 가는 건 아니라고 하니까 Null 오류는 없을듯
        UUID uuid = player.getUniqueId();
        boolean death = playerDeathStatus.getOrDefault(uuid, false);
        if (!death) {
            String guide = "플레이어가 죽었어, 그에 맞는 조언을 한줄로 요약해서 해 줘";
            String message = gpt.getGuide(guide);
            player.sendMessage(message);
            playerDeathStatus.put(uuid, true);
        }
    }
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event){
        Entity entity = event.getEntity();
        Entity damagesource = (Entity) event.getDamageSource(); //불안정 씹음
        if (damagesource instanceof Player player) {
            UUID uuid = player.getUniqueId();
            boolean killed = isKilledStatus.getOrDefault(uuid, false);
            if (!killed) {
            String guide = "플레이어가 "+ entity +" 를 죽였어, 그에 맞는 조언을 한줄로 요약해서 해 줘";
            String message = gpt.getGuide(guide);
            player.sendMessage(message);
            isKilledStatus.put(uuid, true);
            }
        }
    }
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        UUID uuid = player.getUniqueId();
        Inventory inventory = event.getInventory();
        InventoryType inventoryType = inventory.getType();

        // 플레이어별 인벤토리 타입 상태 관리
        Map<InventoryType, Boolean> playerInventoryState = InV.getOrDefault(uuid, new HashMap<>());
        boolean holded = playerInventoryState.getOrDefault(inventoryType, false);

        if (!holded) {
            event.setCancelled(true);
            String guide = "플레이어가 " + inventoryType + "를 열었어! 알맞는 조언을 한 줄로 요약해서 말해 줘";
            String message = gpt.getGuide(guide);
            player.sendMessage(message);

            // 해당 인벤토리 타입의 상태 업데이트
            playerInventoryState.put(inventoryType, true);
            InV.put(uuid, playerInventoryState);
        }
    }
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        UUID uuid = player.getUniqueId();
        Inventory inventory = event.getInventory();
        InventoryType inventoryType = inventory.getType();

        // 플레이어별 인벤토리 타입 상태 관리
        Map<InventoryType, Boolean> playerInventoryState = InVC.getOrDefault(uuid, new HashMap<>());
        boolean holded = playerInventoryState.getOrDefault(inventoryType, false);

        if (!holded) {
            String guide = "플레이어가 " + inventoryType + "를 닫았어! 알맞는 조언을 한 줄로 요약해서 말해 줘";
            String message = gpt.getGuide(guide);
            player.sendMessage(message);

            // 해당 인벤토리 타입의 상태 업데이트
            playerInventoryState.put(inventoryType, true);
            InVC.put(uuid, playerInventoryState);
        }
    }
    @EventHandler
    public void PlayerItemDamageEvent(PlayerItemDamageEvent event){
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        boolean status = ItemSs.getOrDefault(uuid, false);
        if (!status) {
            String guide = "플레이어의 아이템이 손상됐어! 알맞는 조언을 한 줄로 요약해서 말해 줘";
            String message = gpt.getGuide(guide);
            player.sendMessage(message);
            ItemSs.put(uuid, true);
        }
    }


    //플레이어가 주민을 바라볼 때 1회에 걸쳐 가이드
    //false로 선언하고 마지막에 true로 바꾸면서 다시 안띄우게 하는 식 false일시 띄워주는
}
