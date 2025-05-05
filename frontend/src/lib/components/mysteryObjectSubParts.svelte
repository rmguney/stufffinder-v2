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
            color: subPart.colorHex || '',
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
                color: attributeValues.colorHex,
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
    <div class="flex items-center justify-start mb-4">
        <Button 
            variant="outline" 
            size="sm"
            on:click={() => {
                resetForm();
                showAddForm = !showAddForm;
            }}
            class="text-xs rounded-full px-3 py-1 border-neutral-300 dark:border-neutral-700 hover:bg-neutral-100 dark:hover:bg-neutral-800"
        >
            <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 mr-1.5" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" clip-rule="evenodd" />
            </svg>
            Add Part
        </Button>
    </div>
    
    {#if error}
        <div class="bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-300 p-3 rounded-md mb-4">
            {error}
        </div>
    {/if}
    
    <!-- Add/Edit Form -->
    {#if showAddForm}
        <div class="bg-white dark:bg-neutral-950 border border-neutral-200 dark:border-neutral-700 rounded-md p-4 mb-4">
            <h4 class="text-sm font-medium mb-3 text-neutral-700 dark:text-neutral-300">
                {editingSubPartId ? 'Edit Part' : 'Add New Part'}
            </h4>
            
            <!-- Updated layout: Description and Attributes side-by-side -->
            <div class="flex flex-col lg:flex-row lg:gap-6">
                <!-- Description on the left -->
                <div class="w-full lg:w-1/2 mb-4 lg:mb-0">
                    <label for="description" class="block text-sm font-medium mb-1.5 text-neutral-700 dark:text-neutral-300 flex items-center">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1.5" viewBox="0 0 20 20" fill="currentColor">
                            <path fill-rule="evenodd" d="M4 4a2 2 0 012-2h8a2 2 0 012 2v12a2 2 0 01-2 2H6a2 2 0 01-2-2V4zm2 2a1 1 0 011-1h6a1 1 0 110 2H7a1 1 0 01-1-1zm1 3a1 1 0 100 2h6a1 1 0 100-2H7zm0 3a1 1 0 100 2h6a1 1 0 100-2H7z" clip-rule="evenodd" />
                        </svg>
                        Description*
                    </label>
                    <Textarea 
                        id="description" 
                        class="w-full p-3 border rounded-md text-sm bg-white dark:bg-neutral-950 border-neutral-200 dark:border-neutral-700 focus:ring-2 focus:ring-teal-500/30 focus:border-teal-500 min-h-[100px]" 
                        bind:value={description} 
                        placeholder="Describe this part of the object"
                        rows="3"
                    />
                </div>
                
                <!-- Object Attributes on the right -->
                <div class="w-full lg:w-1/2">
                    <div class="mb-2 text-sm font-medium text-neutral-700 dark:text-neutral-300 flex items-center">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1.5" viewBox="0 0 20 20" fill="currentColor">
                            <path fill-rule="evenodd" d="M17.707 9.293a1 1 0 010 1.414l-7 7a1 1 0 01-1.414 0l-7-7A.997.997 0 012 10V5a3 3 0 013-3h5c.256 0 .512.098.707.293l7 7zM5 6a1 1 0 100-2 1 1 0 000 2z" clip-rule="evenodd" />
                        </svg>
                        Part Attributes
                        <span class="ml-2 px-2 py-0.5 bg-neutral-100 dark:bg-neutral-800 text-xs rounded-full text-neutral-700 dark:text-neutral-300">
                            {activeAttributes.length} added
                        </span>
                    </div>
                    <div class="bg-white dark:bg-neutral-950 border border-neutral-200 dark:border-neutral-700 rounded-md shadow-sm p-3">
                        <!-- Reuse the ObjectAttributes component -->
                        <ObjectAttributes 
                            bind:attributeValues={attributeValues}
                            bind:activeAttributes={activeAttributes}
                            on:update={handleAttributesUpdate}
                            on:valuechange={handleAttributeValueChange}
                        />
                    </div>
                </div>
            </div>
            
            <div class="flex justify-start mt-4 gap-2">
                <Button 
                variant="default" 
                size="sm"
                on:click={saveSubPart}
                class="text-sm py-1 px-4 bg-teal-600 hover:bg-teal-700 text-white rounded-full"
                disabled={isLoading}
            >
                {#if isLoading}
                    <span class="inline-block h-3.5 w-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin mr-1.5"></span>
                    Saving...
                {:else}
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 mr-1.5" viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
                    </svg>
                    Save Part
                {/if}
            </Button>
                <Button 
                    variant="outline" 
                    size="sm"
                    on:click={resetForm}
                    class="text-sm py-1 px-3 rounded-full border-neutral-300 dark:border-neutral-700 hover:bg-neutral-100 dark:hover:bg-neutral-800"
                >
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 mr-1.5" viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm.707-10.293a1 1 0 00-1.414-1.414l-3 3a1 1 0 000 1.414l3 3a1 1 0 001.414-1.414L9.414 11H13a1 1 0 100-2H9.414l1.293-1.293z" clip-rule="evenodd" />
                    </svg>
                    Cancel
                </Button>
            </div>
        </div>
    {/if}
    
    <!-- List of Sub-parts -->
    {#if subParts.length > 0}
        <div class="space-y-3">
            {#each subParts as part (part.id)}
                <div class="bg-white dark:bg-neutral-950 border border-neutral-200 dark:border-neutral-700 rounded-md p-3">
                    <div class="flex justify-between items-start mb-2">
                        <h5 class="font-medium text-sm">{part.description || 'Unnamed Part'}</h5>
                        <div class="flex gap-1">
                            <Button 
                                variant="ghost" 
                                size="xs"
                                on:click={() => editSubPart(part)}
                                class="h-6 w-6 p-0 rounded-full flex items-center justify-center"
                            >
                                <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" viewBox="0 0 20 20" fill="currentColor">
                                    <path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z" />
                                </svg>
                            </Button>
                            <Button 
                                variant="ghost" 
                                size="xs"
                                on:click={() => deleteSubPart(part.id)}
                                class="h-6 w-6 p-0 rounded-full flex items-center justify-center text-red-500 hover:text-red-700 dark:text-red-400 dark:hover:text-red-300 hover:bg-red-50 dark:hover:bg-red-900/30"
                            >
                                <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" viewBox="0 0 20 20" fill="currentColor">
                                    <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd" />
                                </svg>
                            </Button>
                        </div>
                    </div>
                    
                    <!-- Attribute summary - show only filled attributes -->
                    <div class="grid grid-cols-2 gap-x-4 gap-y-1 text-xs mt-2">
                        {#if part.material}
                            <div class="flex justify-between"><span class="text-neutral-500">Material:</span> <span>{part.material}</span></div>
                        {/if}
                        {#if part.color}
                            <div class="flex justify-between items-center">
                                <span class="text-neutral-500">Color:</span> 
                                <span class="flex items-center">
                                    <span class="w-3 h-3 inline-block rounded-full border mr-1" style="background-color: {part.color};"></span>
                                    <span>{part.color}</span>
                                </span>
                            </div>
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
        <div class="bg-white dark:bg-neutral-950 border border-neutral-200 dark:border-neutral-700 rounded-md p-4 text-center text-sm text-neutral-500">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-10 w-10 mx-auto mb-2 text-neutral-400" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M5 4a3 3 0 00-3 3v6a3 3 0 003 3h10a3 3 0 003-3V7a3 3 0 00-3-3H5zm-1 9v-1h5v2H5a1 1 0 01-1-1zm7 1h4a1 1 0 001-1v-1h-5v2zm0-4h5V8h-5v2zM9 8H4v2h5V8z" clip-rule="evenodd" />
            </svg>
            <p>No parts added yet. Click "Add Part" to create object sub-parts.</p>
        </div>
    {/if}
</div>
