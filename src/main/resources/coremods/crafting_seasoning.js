var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');

function initializeCoreMod() {
    return {
        'spicecraft_crafting_seasoning': {
            'target': {
                'type': 'METHOD',
                'class': 'net.minecraft.world.inventory.CraftingMenu',
                'methodName': 'm_150546_',
                'methodDesc': '(Lnet/minecraft/world/inventory/AbstractContainerMenu;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/inventory/CraftingContainer;Lnet/minecraft/world/inventory/ResultContainer;)V'
            },
            'transformer': function(method) {
                var descriptor = '(Lnet/minecraft/world/Container;Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;';
                var replacements = 0;
                for (var i = 0; i < method.instructions.size(); i++) {
                    var node = method.instructions.get(i);
                    if (node.getOpcode() == Opcodes.INVOKEINTERFACE
                            && node.owner == 'net/minecraft/world/item/crafting/CraftingRecipe'
                            && node.name == ASMAPI.mapMethod('m_5874_') && node.desc == descriptor) {
                        method.instructions.set(node, new MethodInsnNode(Opcodes.INVOKESTATIC,
                            'com/spicecraft/crafting/SeasoningCrafting', 'assemble',
                            '(Lnet/minecraft/world/item/crafting/CraftingRecipe;Lnet/minecraft/world/Container;Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;', false));
                        replacements++;
                    }
                }
                if (replacements != 1) ASMAPI.log('WARN', 'SpiceCraft: expected one crafting assembly call, found ' + replacements + '. A crafting compatibility integration may be required.');
                return method;
            }
        }
    };
}
