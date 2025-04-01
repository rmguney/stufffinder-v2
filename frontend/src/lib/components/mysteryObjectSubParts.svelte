<script>
    import { Button } from "$lib/components/ui/button";
    import { Textarea } from "$lib/components/ui/textarea";
    import { Card } from "$lib/components/ui/card";
    import ObjectAttributes from "./objectAttributes.svelte";
    import { createEventDispatcher } from 'svelte';
    import { addSubPart, removeSubPart, updateSubPart } from "$lib/utils/mysteryObjectUtils.js";
    
    const dispatch = createEventDispatcher();
    
    // Props for parent mystery object
    export let mysteryObjectId = null; // Can be null for new posts
    export let subParts = [];
    
    // Local state
    let showAddForm = false;
    let editingSubPartId = null;
    let isLoading = false;
    let error = null;
    
    // Form state for new/editing sub-part
    let description = '';
    let attributeValues = {};
    let activeAttributes = [];
    
    // Reset form
    function resetForm() {
        description = '';
        attributeValues = {};
        activeAttributes = [];
        editingSubPartId = null;
        showAddForm = false;
    }
    
    // Handle attribute updates from ObjectAttributes component
    function handleAttributesUpdate(event) {
        activeAttributes = event.detail.activeAttributes;
    }
    
    // Handle attribute value changes
    function handleAttributeValueChange(event) {
        attributeValues = event.detail.attributeValues;
    }
    
    // Start editing a sub-part
    function editSubPart(subPart) {
        editingSubPartId = subPart.id;
        description = subPart.description || '';
        
        // Set attributes from sub-part
        attributeValues = {
            material: subPart.material || '',
            shape: subPart.shape || '',
            color: subPart.color || '',
            texture: subPart.texture || '',
            writtenText: subPart.writtenText || '',
            descriptionOfParts: subPart.descriptionOfParts || '',
            location: subPart.location || '',
            hardness: subPart.hardness || '',
            timePeriod: subPart.timePeriod || '',
            smell: subPart.smell || '',
            taste: subPart.taste || '',
            functionality: subPart.functionality || '',
            markings: subPart.markings || '',
            pattern: subPart.pattern || '',
            brand: subPart.brand || '',
            print: subPart.print || '',
            imageLicenses: subPart.imageLicenses || '',
            value: subPart.value || '',
            originOfAcquisition: subPart.originOfAcquisition || '',
            weight: subPart.weight || '',
            handmade: subPart.handmade || false,
            oneOfAKind: subPart.oneOfAKind || false,
            sizeX: subPart.sizeX || '',
            sizeY: subPart.sizeY || '',
            sizeZ: subPart.sizeZ || ''
        };
        
        // Determine active attributes based on what's filled in
        activeAttributes = Object.keys(attributeValues).filter(key => {
            const value = attributeValues[key];
            return value !== '' && value !== null && value !== undefined;
        });
        
        showAddForm = true;
    }
    
    // Save a sub-part (new or edited)
    async function saveSubPart() {
        if (!description.trim()) {
            error = 'Description is required';
            return;
        }
        
        isLoading = true;
        error = null;
        
        try {
            // Build the sub-part object from form data
            const subPartData = {
                description,
                material: attributeValues.material,
                color: attributeValues.color,
                shape: attributeValues.shape,
                location: attributeValues.location,
                smell: attributeValues.smell,
                taste: attributeValues.taste,
                texture: attributeValues.texture,
                functionality: attributeValues.functionality,
                markings: attributeValues.markings,
                handmade: attributeValues.handmade || false,
                oneOfAKind: attributeValues.oneOfAKind || false,
                weight: parseFloat(attributeValues.weight) || null,
                timePeriod: attributeValues.timePeriod,
                writtenText: attributeValues.writtenText,
                descriptionOfParts: attributeValues.descriptionOfParts,
                hardness: attributeValues.hardness,
                value: parseFloat(attributeValues.value) || null,
                originOfAcquisition: attributeValues.originOfAcquisition,
                pattern: attributeValues.pattern,
                brand: attributeValues.brand,
                print: attributeValues.print,
                imageLicenses: attributeValues.imageLicenses,
                sizeX: parseFloat(attributeValues.sizeX) || null,
                sizeY: parseFloat(attributeValues.sizeY) || null,
                sizeZ: parseFloat(attributeValues.sizeZ) || null,
                item_condition: attributeValues.itemCondition || null
            };
            
            let result;
            if (mysteryObjectId === null) {
                // For new post creation, just add to the local array
                // Generate a temporary ID for UI purposes
                const tempId = Date.now();
                result = {
                    ...subPartData,
                    id: tempId
                };
                
                // Add to the local list
                subParts = [...subParts, result];
            } else if (editingSubPartId) {
                // Update existing sub-part
                result = await updateSubPart(mysteryObjectId, editingSubPartId, subPartData);
                
                // Update the local list
                subParts = subParts.map(part => 
                    part.id === editingSubPartId ? result : part
                );
            } else {
                // Add new sub-part to an existing mystery object
                result = await addSubPart(mysteryObjectId, subPartData);
                
                // Add to the local list
                subParts = [...subParts, result];
            }
            
            // Notify parent component about the change
            dispatch('update', { subParts });
            
            // Reset form
            resetForm();
        } catch (err) {
            console.error('Error saving sub-part:', err);
            error = err.message || 'Failed to save sub-part';
        } finally {
            isLoading = false;
        }
    }
    
    // Delete a sub-part
    async function deleteSubPart(subPartId) {
        if (!confirm('Are you sure you want to delete this part?')) {
            return;
        }
        
        isLoading = true;
        error = null;
        
        try {
            if (mysteryObjectId === null) {
                // For new post creation, just remove from the local array
                subParts = subParts.filter(part => part.id !== subPartId);
            } else {
                // For existing mystery objects, call the API
                await removeSubPart(mysteryObjectId, subPartId);
                
                // Update the local list
                subParts = subParts.filter(part => part.id !== subPartId);
            }
            
            // Notify parent component about the change
            dispatch('update', { subParts });
        } catch (err) {
            console.error('Error deleting sub-part:', err);
            error = err.message || 'Failed to delete sub-part';
        } finally {
            isLoading = false;
        }
    }
</script>

<div class="mb-6">
    <div class="flex items-center justify-between mb-4">
        <h3 class="text-lg font-medium">Object Parts</h3>
        <Button 
            variant="outline" 
            size="sm"
            on:click={() => {
                resetForm();
                showAddForm = !showAddForm;
            }}
            class="text-xs"
        >
            {#if showAddForm}
                Cancel
            {:else}
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="mr-1"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
                Add Part
            {/if}
        </Button>
    </div>
    
    {#if error}
        <div class="bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-300 p-3 rounded-md mb-4">
            {error}
        </div>
    {/if}
    
    <!-- Add/Edit Form -->
    {#if showAddForm}
        <div class="bg-neutral-50 dark:bg-neutral-900 border border-neutral-200 dark:border-neutral-700 rounded-md p-4 mb-4">
            <h4 class="text-sm font-medium mb-3">
                {editingSubPartId ? 'Edit Part' : 'Add New Part'}
            </h4>
            
            <div class="mb-4">
                <label for="description" class="block text-sm font-medium mb-1.5">Description*</label>
                <Textarea 
                    id="description" 
                    class="w-full p-2 border rounded dark:border-gray-600 h-20" 
                    bind:value={description} 
                    placeholder="Describe this part of the object"
                />
            </div>
            
            <!-- Reuse the ObjectAttributes component -->
            <ObjectAttributes 
                bind:attributeValues={attributeValues}
                bind:activeAttributes={activeAttributes}
                on:update={handleAttributesUpdate}
                on:valuechange={handleAttributeValueChange}
            />
            
            <div class="flex justify-end mt-4 gap-2">
                <Button 
                    variant="outline" 
                    size="sm"
                    on:click={resetForm}
                    class="text-xs"
                >
                    Cancel
                </Button>
                <Button 
                    variant="default" 
                    size="sm"
                    on:click={saveSubPart}
                    class="text-xs"
                    disabled={isLoading}
                >
                    {#if isLoading}
                        <span class="inline-block h-4 w-4 border-2 border-current/30 border-t-current rounded-full animate-spin mr-2"></span>
                        Saving...
                    {:else}
                        Save Part
                    {/if}
                </Button>
            </div>
        </div>
    {/if}
    
    <!-- List of Sub-parts -->
    {#if subParts.length > 0}
        <div class="space-y-3">
            {#each subParts as part (part.id)}
                <div class="bg-white dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 rounded-md p-3">
                    <div class="flex justify-between items-start mb-2">
                        <h5 class="font-medium text-sm">{part.description || 'Unnamed Part'}</h5>
                        <div class="flex gap-1">
                            <Button 
                                variant="ghost" 
                                size="xs"
                                on:click={() => editSubPart(part)}
                                class="h-6 px-2 text-xs"
                            >
                                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 3a2.85 2.85 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"></path></svg>
                            </Button>
                            <Button 
                                variant="ghost" 
                                size="xs"
                                on:click={() => deleteSubPart(part.id)}
                                class="h-6 px-2 text-xs text-red-500 hover:text-red-700 dark:text-red-400 dark:hover:text-red-300"
                            >
                                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
                            </Button>
                        </div>
                    </div>
                    
                    <!-- Attribute summary - show only filled attributes -->
                    <div class="grid grid-cols-2 gap-x-4 gap-y-1 text-xs mt-2">
                        {#if part.material}
                            <div class="flex justify-between"><span class="text-neutral-500">Material:</span> <span>{part.material}</span></div>
                        {/if}
                        {#if part.color}
                            <div class="flex justify-between"><span class="text-neutral-500">Color:</span> <span>{part.color}</span></div>
                        {/if}
                        {#if part.shape}
                            <div class="flex justify-between"><span class="text-neutral-500">Shape:</span> <span>{part.shape}</span></div>
                        {/if}
                        {#if part.texture}
                            <div class="flex justify-between"><span class="text-neutral-500">Texture:</span> <span>{part.texture}</span></div>
                        {/if}
                        <!-- Add more attributes as needed -->
                    </div>
                </div>
            {/each}
        </div>
    {:else}
        <div class="bg-neutral-50 dark:bg-neutral-900 border border-neutral-200 dark:border-neutral-700 rounded-md p-4 text-center text-sm text-neutral-500">
            No parts added yet. Click "Add Part" to create object sub-parts.
        </div>
    {/if}
</div>
