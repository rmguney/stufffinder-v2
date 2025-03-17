<script>
    import * as Card from "$lib/components/ui/card/index.js";
    import { Button } from "$lib/components/ui/button";
    import Query from '$lib/components/query.svelte';
    import { threadStore } from "../../threadStore";
    import { goto } from '$app/navigation';
    import { activeUser } from '../../userStore';
    import { Input } from "$lib/components/ui/input/index.js";
    import { Textarea } from "$lib/components/ui/textarea/index.js";
    import { PUBLIC_API_URL } from "$env/static/public";

    // Replace single imageFile with array of media files
    let mediaFiles = [];
    let title = '';
    let tags = [];
    let labels = [];
    let postedBy;
    let description = '';
    let anonymous = false; 
    let resolved = false;
    
    // Store all attribute values in a single object
    let attributeValues = {
        material: '',
        shape: '',
        color: '',
        texture: '',
        weight: '',
        smell: '',
        taste: '',
        marking: '',
        functionality: '',
        period: '',
        location: '',
        writtenText: '',
        descriptionOfParts: '',
        hardness: '',
        value: '',
        originOfAcquisition: '',
        pattern: '',
        brand: '',
        print: '',
        imageLicenses: '',
        sizeX: '',
        sizeY: '',
        sizeZ: '',
        itemCondition: '',
        handmade: false,
        oneOfAKind: false
    };
    
    // Track which attributes are currently shown
    let activeAttributes = [];
    
    // Whether attribute selector dropdown is open
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
        }
        showAttributeSelector = false;
    }
    
    // Function to remove an attribute from the form
    function removeAttribute(attrId) {
        activeAttributes = activeAttributes.filter(attr => attr !== attrId);
    }
    
    // Get available (not yet added) attributes
    $: availableToAdd = availableAttributes.filter(attr => !activeAttributes.includes(attr.id));
    
    let errors = {
        title: '',
        media: '', // Changed from image to media
        description: ''
    };

    $: postedBy = $activeUser;

    async function fetchLabelForQcode(qcode) {
        const wikidataApiUrl = `https://www.wikidata.org/w/api.php?action=wbgetentities&ids=${qcode}&format=json&languages=en&origin=*`;
        try {
            const response = await fetch(wikidataApiUrl);
            const data = await response.json();
            const label = data.entities[qcode]?.labels?.en?.value;
            if (!label) throw new Error(`Label not found for qcode: ${qcode}`);
            return label;
        } catch (error) {
            console.error('Error fetching label:', error);
            return null;
        }
    }

    async function enrichTagsWithLabels(tags) {
        const enrichedTags = [];
        for (const tag of tags) {
            const label = await fetchLabelForQcode(tag.id);
            if (label) {
                enrichedTags.push({ id: tag.id, label });
            }
        }
        return enrichedTags;
    }
    
    // Function to handle media file selection
    function handleMediaAdd(event) {
        const files = event.target.files;
        if (files && files.length > 0) {
            // Add the new files to our media collection
            const newFiles = Array.from(files).map(file => {
                const fileType = file.type.split('/')[0]; // 'image', 'video', 'audio', etc.
                return {
                    file: file,
                    type: fileType,
                    url: URL.createObjectURL(file),
                    name: file.name
                };
            });
            mediaFiles = [...mediaFiles, ...newFiles];
        }
        // Reset the input so the same file can be selected again if needed
        event.target.value = '';
    }
    
    // Function to remove a file from the mediaFiles array
    function removeMediaFile(index) {
        URL.revokeObjectURL(mediaFiles[index].url); // Clean up the created URL
        mediaFiles = mediaFiles.filter((_, i) => i !== index);
    }
    
    let handlePost = async () => {
        // Reset error state
        errors = {
            title: '',
            media: '',
            description: ''
        };
        
        // Validate required fields
        let hasErrors = false;
        
        if (!title.trim()) {
            errors.title = 'Title is required';
            hasErrors = true;
        }
        
        if (mediaFiles.length === 0) {
            errors.media = 'At least one media file is required';
            hasErrors = true;
        }
        
        if (!description.trim()) {
            errors.description = 'Description is required';
            hasErrors = true;
        }
        
        if (hasErrors) {
            return; // Don't proceed if validation fails
        }
    
        try {
            const enrichedTags = await enrichTagsWithLabels(tags);
            
            // Create mysteryObject using attributeValues
            const mysteryObject = {
                description,
                material: attributeValues.material,
                color: attributeValues.color,
                shape: attributeValues.shape,
                location: attributeValues.location,
                smell: attributeValues.smell,
                taste: attributeValues.taste || attributeValues.smell,
                texture: attributeValues.texture,
                functionality: attributeValues.functionality,
                markings: attributeValues.marking,
                handmade: attributeValues.handmade || false,
                oneOfAKind: attributeValues.oneOfAKind || false,
                weight: parseFloat(attributeValues.weight) || null,
                timePeriod: attributeValues.period,
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
            
            // Create FormData for multipart request
            const formData = new FormData();
            formData.append('title', title);
            formData.append('content', description);
            
            // Add tags
            if (tags && tags.length > 0) {
                const tagIds = tags.map(tag => tag.id);
                formData.append('tags', new Blob([JSON.stringify(tagIds)], {
                    type: 'application/json'
                }));
            }
            
            // Add mystery object as JSON string
            formData.append('mysteryObject', new Blob([JSON.stringify(mysteryObject)], {
                type: 'application/json'
            }));
    
            // Create post first
            const response = await fetch(`${PUBLIC_API_URL}/api/posts/create`, {
                method: 'POST',
                body: formData,
                credentials: 'include'
            });
    
            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Failed to create post');
            }
            
            const responseData = await response.json();
            
            // BACKWARD COMPATIBILITY: Upload first image to old endpoint if it exists
            const firstImage = mediaFiles.find(item => item.type === 'image');
            if (firstImage) {
                const imageFormData = new FormData();
                imageFormData.append('image', firstImage.file);
                
                try {
                    const imageResponse = await fetch(`${PUBLIC_API_URL}/api/mysteryObjects/${responseData.mysteryObjectId}/upload-image`, {
                        method: 'POST',
                        body: imageFormData,
                        credentials: 'include'
                    });
                    
                    if (!imageResponse.ok) {
                        console.warn('Failed to upload image to legacy endpoint, but continuing...');
                    }
                } catch (imageError) {
                    console.warn('Error uploading to legacy endpoint:', imageError);
                    // Continue with new uploads
                }
            }
            
            // If we have media files, upload them with the new endpoint
            if (mediaFiles.length > 0) {
                for (let i = 0; i < mediaFiles.length; i++) {
                    const mediaItem = mediaFiles[i];
                    const mediaFormData = new FormData();
                    mediaFormData.append('file', mediaItem.file);
                    mediaFormData.append('type', mediaItem.type);
                    
                    const mediaResponse = await fetch(`${PUBLIC_API_URL}/api/mysteryObjects/${responseData.mysteryObjectId}/upload-media`, {
                        method: 'POST',
                        body: mediaFormData,
                        credentials: 'include'
                    });
            
                    if (!mediaResponse.ok) {
                        console.error(`Failed to upload media file ${i+1}`);
                        // Continue with other uploads even if one fails
                    }
                }
            }
    
            threadStore.update(prev => [...prev, responseData]);
            goto('/');
        } catch (error) {
            console.error('Error creating post:', error);
            alert(`Failed to create post: ${error.message}`);
        }
    };

    // Clean up object URLs when component is destroyed
    import { onDestroy } from 'svelte';
    
    onDestroy(() => {
        // Clean up any created object URLs
        for (const media of mediaFiles) {
            if (media.url) {
                URL.revokeObjectURL(media.url);
            }
        }
    });

    // Add state for color picker
    let selectedHexColor = "#ffffff";
    let isLoadingColorName = false;
    
    // Function to fetch color name from TheColorAPI
    async function fetchColorName(hexColor) {
        isLoadingColorName = true;
        // Remove the # character from hex color
        const cleanHex = hexColor.replace('#', '');
        
        try {
            const response = await fetch(`https://www.thecolorapi.com/id?hex=${cleanHex}`);
            if (!response.ok) {
                throw new Error('Failed to fetch color name');
            }
            const data = await response.json();
            
            // Update color value with the name
            attributeValues.color = data.name.value || 'Unknown color';
        } catch (error) {
            console.error('Error fetching color name:', error);
            attributeValues.color = `Color #${cleanHex}`;
        } finally {
            isLoadingColorName = false;
        }
    }
    
    function handleColorChange(event) {
        selectedHexColor = event.target.value;
        fetchColorName(selectedHexColor);
    }

    // Add this helper function for consistent removal icon
    function RemovalIcon() {
        return `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>`;
    }
</script>

<div class="flex justify-center p-3 sm:p-6 lg:py-10 bg-change dark:bg-dark shifting">
    <form class="w-full lg:w-2/3" on:submit|preventDefault={handlePost}>
        <Card.Root class="bg-opacity-90 shadow-xl">
            <Card.Title class="p-3 sm:p-4 text-xl sm:text-2xl mt-4 sm:mt-6 text-center">
                Let's help you post new stuff
                <small class="block text-xs sm:text-sm mt-1 sm:mt-2 font-semibold opacity-80">Fill in the details about your object, you can leave any blank if you are not sure, but try to be as precise as possible</small>
            </Card.Title>
        <div class="bg-opacity-95 rounded-lg shadow-lg p-4 sm:p-6">

            <!-- Title -->
            <div class="mb-5">
                <label for="title" class="block text-sm font-medium mb-1.5">Title*</label>
                <Textarea id="title" class="w-full p-2 border rounded dark:border-gray-600 h-auto text-base" bind:value={title} placeholder="This is what people will see on their homepage so try to make it interesting" />
                {#if errors.title}
                    <p class="text-red-500 text-sm mt-1">{errors.title}</p>
                {/if}
            </div>

            <!-- Object Attributes Section -->
            <div class="mb-6">
                <h3 class="text-lg font-medium mb-3 flex items-center">
                    <span>Object Attributes</span>
                    <span class="ml-2 px-2 py-0.5 bg-gray-100 dark:bg-gray-800 text-xs rounded-full">
                        {activeAttributes.length} added
                    </span>
                </h3>
                
                <!-- Add Attribute Button -->
                <Button 
                    variant="outline" 
                    on:click={() => showAttributeSelector = !showAttributeSelector} 
                    class="w-full p-2 border rounded mb-4 transition-all hover:bg-gray-50 dark:hover:bg-neutral-800 flex justify-center items-center gap-2 items-center gap-2">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="feather feather-plus"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
                    Add Attribute
                </Button>
                
                <!-- Attribute Selector Dropdown -->
                {#if showAttributeSelector}
                    <div class="mt-2 p-2 border rounded bg-white dark:bg-neutral-800 shadow-lg max-h-60 overflow-y-auto mb-4 z-10 relative">
                        <div class="sticky top-0 bg-white dark:bg-neutral-800 p-2 border-b mb-1 flex items-center justify-between">
                            <span class="font-medium">Select Attribute</span>
                            <button 
                                class="text-gray-500 hover:text-gray-700 dark:hover:text-gray-300" 
                                on:click={() => showAttributeSelector = false}
                                aria-label="Close selector">
                                ✕
                            </button>
                        </div>
                        {#each availableToAdd as attribute}
                            <button 
                                class="block w-full text-left p-2.5 hover:bg-gray-100 dark:hover:bg-neutral-700 rounded transition-colors"
                                on:click={() => addAttribute(attribute.id)}>
                                {attribute.label}
                            </button>
                        {/each}
                        {#if availableToAdd.length === 0}
                            <p class="p-2 text-gray-500 text-center">All attributes have been added</p>
                        {/if}
                    </div>
                {/if}
                
                <!-- Display Active Attributes -->
                {#if activeAttributes.length > 0}
                    <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-2">
                        {#each activeAttributes as attrId}
                            {@const attr = availableAttributes.find(a => a.id === attrId)}
                            
                            <div class="relative p-3 rounded-md border bg-gray-50 dark:bg-neutral-900 dark:border-gray-700 transition-all hover:shadow-sm {attr.type === 'dimensions' ? 'col-span-1 sm:col-span-2' : ''}">
                                <!-- Updated Remove button with consistent styling -->
                                <button 
                                    type="button" 
                                    class="absolute top-2 right-2 h-6 w-6 rounded-full flex items-center justify-center bg-gray-100 text-gray-500 hover:bg-red-50 hover:text-red-500 dark:bg-gray-800 dark:hover:bg-gray-700 transition-colors"
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
                                        class="w-full p-2 rounded border dark:border-gray-600 dark:bg-neutral-950 text-sm">
                                        {#each attr.options as option}
                                            <option value={option.value}>{option.label}</option>
                                        {/each}
                                    </select>
                                
                                {:else if attr.type === "dimensions"}
                                    <div id="dimensions-label" class="block text-sm font-medium mb-3">Dimensions (cm)</div>
                                    <div class="grid grid-cols-3 gap-3" role="group" aria-labelledby="dimensions-label">
                                        <div>
                                            <label for="sizeX" class="block text-xs font-medium mb-1">Length</label>
                                            <Input type="number" id="sizeX" class="w-full p-2 border rounded dark:border-gray-600" bind:value={attributeValues.sizeX} placeholder="Length (cm)" />
                                        </div>
                                        <div>
                                            <label for="sizeY" class="block text-xs font-medium mb-1">Width</label>
                                            <Input type="number" id="sizeY" class="w-full p-2 border rounded dark:border-gray-600" bind:value={attributeValues.sizeY} placeholder="Width (cm)" />
                                        </div>
                                        <div>
                                            <label for="sizeZ" class="block text-xs font-medium mb-1">Height</label>
                                            <Input type="number" id="sizeZ" class="w-full p-2 border rounded dark:border-gray-600" bind:value={attributeValues.sizeZ} placeholder="Height (cm)" />
                                        </div>
                                    </div>
                                
                                {:else if attr.type === "checkbox"}
                                    <div class="pt-2">
                                        <div class="flex items-center">
                                            <input 
                                                type="checkbox" 
                                                bind:checked={attributeValues[attrId]} 
                                                id={`${attrId}-checkbox`} 
                                                class="w-4 h-4 mr-3" 
                                            />
                                            <label for={`${attrId}-checkbox`} class="text-sm font-medium">
                                                {attr.label}
                                            </label>
                                        </div>
                                    </div>
                                
                                {:else}
                                    <label for={attrId} class="block text-sm font-medium mb-2">{attr.label}</label>
                                    
                                    {#if attrId === "color"}
                                        <!-- Color picker UI -->
                                        <div class="flex flex-col sm:flex-row sm:items-center gap-3">
                                            <input 
                                                type="color"
                                                id="color-picker"
                                                value={selectedHexColor}
                                                on:change={handleColorChange}
                                                class="w-full sm:w-14 h-10 border-none p-0 cursor-pointer rounded"
                                                aria-label="Select color"
                                            />
                                            <div class="flex items-center gap-2 flex-1 mt-2 sm:mt-0 border rounded p-2 bg-white dark:bg-neutral-800">
                                                <div 
                                                    class="w-6 h-6 rounded border"
                                                    style="background-color: {selectedHexColor};"
                                                ></div>
                                                <span class="text-sm truncate">
                                                    {isLoadingColorName ? 'Loading...' : attributeValues.color || 'Select a color'}
                                                </span>
                                            </div>
                                        </div>
                                    {:else}
                                        <Input 
                                            type={attr.type || "text"} 
                                            id={attrId} 
                                            class="w-full p-2 border rounded dark:border-gray-600" 
                                            bind:value={attributeValues[attrId]} 
                                            placeholder={attr.placeholder || ""}
                                        />
                                    {/if}
                                {/if}
                            </div>
                        {/each}
                    </div>
                {/if}
            </div>

            <!-- Description -->
            <div class="mb-5">
                <label for="description" class="block text-sm font-medium mb-1.5">Description*</label>
                <Textarea id="description" class="w-full p-2 mb-4 border rounded dark:border-gray-600 h-32" bind:value={description} placeholder="You can add any additional context about your object or how you came into possession of it."/>
                {#if errors.description}
                    <p class="text-red-500 text-sm mt-1">{errors.description}</p>
                {/if}
            </div>
            
            <!-- Tags -->
            <div class="mb-5">
                <label for="tags" class="block text-sm font-medium mb-1.5">Tags</label>
                
                <!-- Remove the styles for tag dismissal buttons since we've moved that to the Query component -->
                <Query bind:tags={tags} bind:labels={labels} />
            </div>

            <!-- Anonymous Checkbox -->
<!--             <div class="mb-6 flex items-center bg-gray-50 dark:bg-neutral-900 p-3 rounded-md">
                <input 
                    type="checkbox" 
                    bind:checked={anonymous} 
                    id="anonymous-checkbox" 
                    class="w-4 h-4 mr-3" 
                />
                <label for="anonymous-checkbox" class="text-sm font-medium">Post anonymously</label>
            </div> -->

            <!-- Media upload section -->
            <div class="mb-6">
                <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between mb-2">
                    <label for="media-upload" class="block text-sm font-medium">Upload Media Files*</label>
                    <span class="text-xs text-gray-500 mt-1 sm:mt-0">{mediaFiles.length} files selected</span>
                </div>
                
                <div class="border-2 border-dashed rounded-lg p-4 text-center mb-3 bg-gray-50 dark:bg-neutral-900 dark:border-gray-700 hover:bg-gray-100 dark:hover:bg-neutral-800 transition-colors">
                    <label for="media-upload" class="cursor-pointer block">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-10 w-10 mx-auto text-gray-400 mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                        </svg>
                        <!-- Different text based on screen size -->
                        <span class="text-sm block sm:hidden">Tap to select files</span>
                        <span class="text-sm hidden sm:block lg:hidden">Click to select files</span>
                        <span class="text-sm hidden lg:block">Click to select files or drag and drop</span>
                        <p class="text-xs text-gray-500 mt-1">Images, videos, or audio files</p>
                        <input 
                            type="file" 
                            id="media-upload" 
                            accept="image/*,video/*,audio/*" 
                            on:change={handleMediaAdd}
                            class="hidden"
                            multiple
                        />
                    </label>
                </div>
                
                {#if errors.media}
                    <p class="text-red-500 text-sm mb-3">{errors.media}</p>
                {/if}
                
                <!-- Media Files Preview -->
                {#if mediaFiles.length > 0}
                    <div class="mt-4 grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3">
                        {#each mediaFiles as mediaItem, index}
                            <div class="relative border rounded overflow-hidden bg-white dark:bg-neutral-800 shadow-sm hover:shadow transition-all">
                                <!-- Add consistent removal button to the top-right corner, similar to attributes -->
                                <button 
                                    type="button"
                                    class="absolute top-2 right-2 z-10 h-6 w-6 rounded-full flex items-center justify-center bg-black bg-opacity-50 text-white hover:bg-red-500 transition-colors"
                                    on:click={() => removeMediaFile(index)}
                                    title="Remove media"
                                >
                                    {@html RemovalIcon()}
                                </button>

                                <!-- Media preview with consistent height -->
                                <div class="aspect-square overflow-hidden bg-gray-100 dark:bg-neutral-900 flex items-center justify-center">
                                    {#if mediaItem.type === 'image'}
                                        <img src={mediaItem.url} alt="Preview" class="w-full h-full object-cover" />
                                    {:else if mediaItem.type === 'video'}
                                        <video src={mediaItem.url} class="w-full h-full object-cover" controls></video>
                                    {:else if mediaItem.type === 'audio'}
                                        <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zM9 10l12-3" />
                                        </svg>
                                    {:else}
                                        <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                                        </svg>
                                    {/if}
                                </div>
                                
                                <!-- File name and controls -->
                                <div class="p-2 border-t">
                                    <div class="text-xs truncate font-medium">{mediaItem.name}</div>
                                    <div class="mt-1">
                                        <span class="text-xs text-gray-500 capitalize">{mediaItem.type}</span>
                                    </div>
                                </div>
                            </div>
                        {/each}
                    </div>
                {/if}
            </div>

            <!-- Submit Button -->
            <div>
                <Button
                on:click={handlePost}
                variant="outline"
                size="icon"
                class="w-full p-2 border rounded mb-4 transition-all hover:bg-rose-900 flex justify-center items-center gap-2">
                Post Your Stuff
                </Button>
            </div>
        </div>
    </Card.Root>
    </form>
</div>