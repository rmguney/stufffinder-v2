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
            
            // If we have media files, upload them separately
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
</script>

<div class="flex justify-center p-6 lg:py-10 bg-change dark:bg-dark shifting">
    <form class="w-full lg:w-2/3" on:submit|preventDefault={handlePost}>
        <Card.Root class="bg-opacity-90">
            <Card.Title class="p-4 text-2xl mt-6 text-center">
                Let's help you post new stuff
                <small class="block text-sm mt-2 font-semibold">Fill in the details about your object, you can leave any blank if you are not sure, but try to be as precise as possible</small>
            </Card.Title>
        <div class="bg-opacity-95 rounded-lg shadow-lg p-6">

            <!-- Title -->
            <div class="mb-4">
                <label for="title" class="block text-sm font-medium mb-2">Title*</label>
                <Textarea id="title" class="w-full p-2 border rounded dark:border-gray-600 h-auto" bind:value={title} placeholder="This is what people will see on their homepage so try to make it interesting" />
                {#if errors.title}
                    <p class="text-red-500 text-sm mt-1">{errors.title}</p>
                {/if}
            </div>

            <!-- Object Attributes Section -->
            <div class="mb-4">
                <h3 class="text-lg font-medium mb-2">Object Attributes</h3>
                
                <!-- Add Attribute Button -->
                <Button 
                    variant="outline" 
                    on:click={() => showAttributeSelector = !showAttributeSelector} 
                    class="w-full p-2 border rounded mb-4">
                    Add Attribute
                </Button>
                
                <!-- Attribute Selector Dropdown -->
                {#if showAttributeSelector}
                    <div class="mt-2 p-2 border rounded bg-white dark:bg-neutral-800 shadow-lg max-h-60 overflow-y-auto mb-4">
                        {#each availableToAdd as attribute}
                            <button 
                                class="block w-full text-left p-2 hover:bg-gray-100 dark:hover:bg-neutral-700 rounded"
                                on:click={() => addAttribute(attribute.id)}>
                                {attribute.label}
                            </button>
                        {/each}
                        {#if availableToAdd.length === 0}
                            <p class="p-2 text-gray-500">All attributes have been added</p>
                        {/if}
                    </div>
                {/if}
                
                <!-- Display Active Attributes -->
                <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
                    {#each activeAttributes as attrId}
                        {@const attr = availableAttributes.find(a => a.id === attrId)}
                        
                        {#if attr.type === "select"}
                            <div class="relative">
                                <label for={attrId} class="block text-sm font-medium mb-2">
                                    {attr.label}
                                    <button type="button" class="ml-2 text-red-500" on:click={() => removeAttribute(attrId)} title="Remove attribute">×</button>
                                </label>
                                <select 
                                    id={attrId} 
                                    bind:value={attributeValues[attrId]} 
                                    class="w-full p-2 rounded border dark:border-gray-600 dark:bg-neutral-950 text-sm">
                                    {#each attr.options as option}
                                        <option value={option.value}>{option.label}</option>
                                    {/each}
                                </select>
                            </div>
                        
                        {:else if attr.type === "dimensions"}
                            <div class="relative col-span-2">
                                <div id="dimensions-label" class="block text-sm font-medium mb-2">
                                    Dimensions (cm)
                                    <button type="button" class="ml-2 text-red-500" on:click={() => removeAttribute(attrId)} title="Remove attribute">×</button>
                                </div>
                                <div class="grid grid-cols-3 gap-2" role="group" aria-labelledby="dimensions-label">
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
                            </div>
                        
                        {:else if attr.type === "checkbox"}
                            <div class="relative">
                                <div class="flex items-center">
                                    <input 
                                        type="checkbox" 
                                        bind:checked={attributeValues[attrId]} 
                                        id={`${attrId}-checkbox`} 
                                        class="mr-2" 
                                    />
                                    <label for={`${attrId}-checkbox`} class="text-sm">
                                        {attr.label}
                                        <button type="button" class="ml-2 text-red-500" on:click={() => removeAttribute(attrId)} title="Remove attribute">×</button>
                                    </label>
                                </div>
                            </div>
                        
                        {:else}
                            <div class="relative">
                                <label for={attrId} class="block text-sm font-medium mb-2">
                                    {attr.label}
                                    <button type="button" class="ml-2 text-red-500" on:click={() => removeAttribute(attrId)} title="Remove attribute">×</button>
                                </label>
                                <Input 
                                    type={attr.type || "text"} 
                                    id={attrId} 
                                    class="w-full p-2 border rounded dark:border-gray-600" 
                                    bind:value={attributeValues[attrId]} 
                                    placeholder={attr.placeholder || ""}
                                />
                            </div>
                        {/if}
                    {/each}
                </div>
            </div>

            <!-- Description -->
            <div class="mb-4">
                <label for="description" class="block text-sm font-medium mb-2">Description*</label>
                <Textarea id="description" class="w-full p-2 mb-4 border rounded dark:border-gray-600 h-auto" bind:value={description} placeholder="You can add any additional context about your object or how you came into possession of it."/>
                {#if errors.description}
                    <p class="text-red-500 text-sm mt-1">{errors.description}</p>
                {/if}
            </div>
            
            <!-- Tags -->
            <div class="mb-4">
                <label for="tags" class="block text-sm font-medium mb-2">Tags</label>
                <Query bind:tags={tags} bind:labels={labels} />
            </div>

            <!-- Anonymous Checkbox -->
            <div class="mb-6 flex items-center">
                <input type="checkbox" bind:checked={anonymous} id="anonymous-checkbox" class="mr-2" />
                <label for="anonymous-checkbox" class="text-sm">Post anonymously</label>
            </div>

            <!-- Media upload section -->
            <div class="mb-6">
                <label for="media-upload" class="block text-sm font-medium mb-2">Upload Media Files*</label>
                <input 
                    type="file" 
                    id="media-upload" 
                    accept="image/*,video/*,audio/*" 
                    on:change={handleMediaAdd}
                    class="w-full p-2 border rounded dark:border-gray-600"
                    multiple
                />
                {#if errors.media}
                    <p class="text-red-500 text-sm mt-1">{errors.media}</p>
                {/if}
                <p class="text-xs text-gray-500 mt-1">Upload images, videos, or audio files of your mystery object (at least one required)</p>
                
                <!-- Media Files Preview -->
                {#if mediaFiles.length > 0}
                    <div class="mt-4 grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
                        {#each mediaFiles as mediaItem, index}
                            <div class="relative border rounded p-2 bg-gray-50 dark:bg-neutral-800">
                                <!-- Preview based on media type -->
                                {#if mediaItem.type === 'image'}
                                    <img src={mediaItem.url} alt="Preview" class="w-full h-32 object-cover rounded" />
                                {:else if mediaItem.type === 'video'}
                                    <video src={mediaItem.url} controls class="w-full h-32 object-cover rounded"></video>
                                {:else if mediaItem.type === 'audio'}
                                    <audio src={mediaItem.url} controls class="w-full mt-2"></audio>
                                    <div class="text-center p-4">
                                        <svg xmlns="http://www.w3.org/2000/svg" class="h-16 w-16 mx-auto text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zM9 10l12-3" />
                                        </svg>
                                    </div>
                                {:else}
                                    <div class="text-center p-4 h-32 flex items-center justify-center">
                                        <span>{mediaItem.name}</span>
                                    </div>
                                {/if}
                                
                                <!-- File name and remove button -->
                                <div class="mt-2 text-xs truncate">{mediaItem.name}</div>
                                <button 
                                    type="button"
                                    class="absolute top-1 right-1 bg-red-500 text-white rounded-full w-6 h-6 flex items-center justify-center"
                                    on:click={() => removeMediaFile(index)}
                                >
                                    ×
                                </button>
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
                class="w-full p-4 bg-black dark:bg-white text-white dark:text-black hover:bg-rose-900 hover:dark:bg-rose-900 hover:text-white transition-colors rounded shadow">
                Post Your Stuff
                </Button>
            </div>
        </div>
    </Card.Root>
    </form>
</div>