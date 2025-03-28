<script>
    import { Button } from "$lib/components/ui/button";
    import { Input } from "$lib/components/ui/input";
    import ColorPicker from "./colorPicker.svelte";
    import MaterialSelector from "./materialSelector.svelte";
    import { createEventDispatcher } from 'svelte';
    
    const dispatch = createEventDispatcher();
    
    // Props from parent
    export let attributeValues = {};
    export let activeAttributes = [];
    
    // Local state
    let showAttributeSelector = false;
    
    // Define all available attributes with their metadata
    const availableAttributes = [
        { id: "material", label: "Material", placeholder: "E.g., wood, metal, plastic, fabric" },
        { id: "shape", label: "Shape", placeholder: "E.g., round, square or something more elaborate" },
        { id: "color", label: "Color", placeholder: "E.g., red, blue, yellow, transparent" },
        { id: "texture", label: "Texture", placeholder: "E.g., smooth, rough, bumpy" },
        { id: "marking", label: "Markings", placeholder: "Does it have logo, text, engravings etc?" },
        { id: "smell", label: "Smell", placeholder: "E.g., sweet, odorless, pungent" },
        { id: "functionality", label: "Functionality", placeholder: "E.g., cutting, writing, art" },
        { id: "period", label: "Time Period", placeholder: "E.g., 1800s, 1900s, 2000s" },
        { id: "location", label: "Location", placeholder: "Where is it typically found? E.g., Europe, Asia" },
        { id: "writtenText", label: "Written Text", placeholder: "Any writing on the object" },
        { id: "hardness", label: "Hardness", placeholder: "E.g., soft, hard, flexible" },
        { id: "value", label: "Price ($)", placeholder: "Estimated value in dollars", type: "number" },
        { id: "originOfAcquisition", label: "Origin of Acquisition", placeholder: "Where/how you got it" },
        { id: "pattern", label: "Pattern", placeholder: "E.g., striped, checkered, floral" },
        { id: "print", label: "Print", placeholder: "Any printed design or imagery" },
        { id: "dimensions", label: "Dimensions", placeholder: "Size measurements", type: "dimensions" },
        { id: "weight", label: "Weight (grams)", placeholder: "Weight in grams", type: "number" },
        { id: "handmade", label: "Handmade", placeholder: "", type: "checkbox" },
        { id: "oneOfAKind", label: "One of a Kind", placeholder: "", type: "checkbox" }
    ];
    
    // Function to add an attribute to the form
    function addAttribute(attrId) {
        if (!activeAttributes.includes(attrId)) {
            activeAttributes = [...activeAttributes, attrId];
            dispatch('update', { activeAttributes });
        }
        showAttributeSelector = false;
    }
    
    // Function to remove an attribute from the form
    function removeAttribute(attrId) {
        activeAttributes = activeAttributes.filter(attr => attr !== attrId);
        dispatch('update', { activeAttributes });
    }
    
    // Update attribute value and notify parent
    function updateAttributeValue(attrId, value) {
        attributeValues[attrId] = value;
        dispatch('valuechange', { attributeValues });
    }

    // Handle color change specifically to store both name and hex
    function handleColorChange(event) {
        attributeValues.color = event.detail.color;
        attributeValues.colorHex = event.detail.hex; // Store hex separately
        dispatch('valuechange', { attributeValues });
    }

    // Handle material selection
    function handleMaterialChange(event) {
        updateAttributeValue('material', event.detail.value);
    }
    
    // Get available (not yet added) attributes
    $: availableToAdd = availableAttributes.filter(attr => !activeAttributes.includes(attr.id));
    
    // Helper function for consistent removal icon
    function RemovalIcon() {
        return `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>`;
    }
</script>

<div class="mb-5">
    <!-- svelte-ignore a11y-label-has-associated-control -->
    <label class="sm:block text-sm font-medium mb-1.5 flex items-center">
        <span>Object Attributes</span>
        <span class="ml-2 px-2 py-0.5 bg-gray-100 dark:bg-gray-800 text-xs rounded-full">
            {activeAttributes.length} added
        </span>
    </label>
    
    <!-- Add Attribute Button with active state ring -->
    <Button 
        variant="outline" 
        on:click={() => showAttributeSelector = !showAttributeSelector} 
        class="w-full p-2 border dark:border-gray-600 rounded mb-4 transition-all hover:bg-neutral-50 dark:hover:bg-neutral-800 flex justify-center items-center gap-2 
               {showAttributeSelector ? 'ring-2 dark:ring-neutral-50 ring-neutral-800' : ''}"
    >
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="feather feather-plus"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
        <span class="text-sm">Add Attribute</span>
    </Button>
    
    <!-- Attribute Selector Dropdown -->
    {#if showAttributeSelector}
        <div class="mt-2 p-2 border rounded bg-white dark:bg-neutral-950 shadow-lg max-h-60 overflow-y-auto mb-4 z-10 relative">
            <div class="sticky top-0 bg-white dark:bg-neutral-800 p-2 border-b mb-1 flex items-center justify-between">
                <span class="text-sm font-medium">Select Attribute</span>
                <button 
                    class="text-neutral-500 hover:text-neutral-700 dark:hover:text-neutral-300" 
                    on:click={() => showAttributeSelector = false}
                    aria-label="Close selector">
                    ✕
                </button>
            </div>
            {#each availableToAdd as attribute}
                <button 
                    class="block w-full text-left p-2.5 hover:bg-neutral-100 dark:hover:bg-neutral-700 rounded transition-colors text-sm"
                    on:click={() => addAttribute(attribute.id)}>
                    {attribute.label}
                </button>
            {/each}
            {#if availableToAdd.length === 0}
                <p class="p-2 text-sm text-neutral-500 text-center">All attributes have been added</p>
            {/if}
        </div>
    {/if}
    
    <!-- Display Active Attributes -->
    {#if activeAttributes.length > 0}
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-2">
            {#each activeAttributes as attrId}
                {@const attr = availableAttributes.find(a => a.id === attrId)}
                
                <div class="relative p-3 rounded-md border bg-neutral-50 dark:bg-neutral-900 dark:border-neutral-700 transition-all hover:shadow-sm {attr.type === 'dimensions' ? 'col-span-1 sm:col-span-2' : ''}">
                    <!-- Updated Remove button with consistent styling -->
                    <button 
                        type="button" 
                        class="absolute top-2 right-2 h-6 w-6 rounded-full flex items-center justify-center bg-neutral-100 text-neutral-500 hover:bg-red-50 hover:text-red-500 dark:bg-neutral-800 dark:hover:bg-neutral-700 transition-colors"
                        on:click={() => removeAttribute(attrId)}
                        title="Remove attribute"
                    >
                        {@html RemovalIcon()}
                    </button>
                    
                    {#if attr.type === "select"}
                        <label for={attrId} class="block text-sm font-medium mb-2">{attr.label}</label>
                        <select 
                            id={attrId} 
                            bind:value={attributeValues[attrId]} 
                            on:change={() => dispatch('valuechange', { attributeValues })}
                            class="w-full p-2 rounded border dark:border-neutral-600 dark:bg-neutral-950 text-sm">
                            {#each attr.options as option}
                                <option value={option.value}>{option.label}</option>
                            {/each}
                        </select>
                    
                    {:else if attr.type === "dimensions"}
                        <div id="dimensions-label" class="block text-sm font-medium mb-3">Dimensions (cm)</div>
                        <div class="grid grid-cols-3 gap-3" role="group" aria-labelledby="dimensions-label">
                            <div>
                                <label for="sizeX" class="block text-xs font-medium mb-1">Length</label>
                                <Input 
                                    type="number" 
                                    id="sizeX" 
                                    class="w-full p-2 border rounded dark:border-neutral-600" 
                                    bind:value={attributeValues.sizeX}
                                    on:input={() => dispatch('valuechange', { attributeValues })}
                                    placeholder="Length (cm)" 
                                />
                            </div>
                            <div>
                                <label for="sizeY" class="block text-xs font-medium mb-1">Width</label>
                                <Input 
                                    type="number" 
                                    id="sizeY" 
                                    class="w-full p-2 border rounded dark:border-neutral-600" 
                                    bind:value={attributeValues.sizeY}
                                    on:input={() => dispatch('valuechange', { attributeValues })}
                                    placeholder="Width (cm)" 
                                />
                            </div>
                            <div>
                                <label for="sizeZ" class="block text-xs font-medium mb-1">Height</label>
                                <Input 
                                    type="number" 
                                    id="sizeZ" 
                                    class="w-full p-2 border rounded dark:border-neutral-600" 
                                    bind:value={attributeValues.sizeZ}
                                    on:input={() => dispatch('valuechange', { attributeValues })}
                                    placeholder="Height (cm)" 
                                />
                            </div>
                        </div>
                    
                    {:else if attr.type === "checkbox"}
                        <div class="pt-2">
                            <div class="flex items-center">
                                <input 
                                    type="checkbox" 
                                    bind:checked={attributeValues[attrId]} 
                                    on:change={() => dispatch('valuechange', { attributeValues })}
                                    id={`${attrId}-checkbox`} 
                                    class="w-4 h-4 mr-3" 
                                />
                                <label for={`${attrId}-checkbox`} class="text-sm font-medium">
                                    {attr.label}
                                </label>
                            </div>
                        </div>
                    
                    {:else if attrId === "color"}
                        <label for={attrId} class="block text-sm font-medium mb-2">{attr.label}</label>
                        <ColorPicker 
                            bind:value={attributeValues.color}
                            bind:hexValue={attributeValues.colorHex}
                            on:colorchange={handleColorChange}
                        />
                    
                    {:else if attrId === "material"}
                        <label for={attrId} class="block text-sm font-medium mb-2">{attr.label}</label>
                        <MaterialSelector 
                            bind:value={attributeValues.material}
                            on:change={handleMaterialChange}
                            placeholder={attr.placeholder}
                        />
                        
                    {:else}
                        <label for={attrId} class="block text-sm font-medium mb-2">{attr.label}</label>
                        <Input 
                            type={attr.type || "text"} 
                            id={attrId} 
                            class="w-full p-2 border rounded dark:border-neutral-600" 
                            bind:value={attributeValues[attrId]} 
                            on:input={() => dispatch('valuechange', { attributeValues })}
                            placeholder={attr.placeholder || ""}
                        />
                    {/if}
                </div>
            {/each}
        </div>
    {:else}
        <p class="text-sm text-neutral-500 dark:text-gray-400 mt-2">No attributes added yet. Click 'Add Attribute' to start.</p>
    {/if}
</div>
