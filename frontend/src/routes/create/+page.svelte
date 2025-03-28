<script>
    import * as Card from "$lib/components/ui/card/index.js";
    import { Button } from "$lib/components/ui/button";
    import Query from '$lib/components/query.svelte';
    import { threadStore } from "../../threadStore";
    import { goto } from '$app/navigation';
    import { activeUser } from '../../userStore';
    import { Textarea } from "$lib/components/ui/textarea/index.js";
    import { PUBLIC_API_URL } from "$env/static/public";
    import MediaUploader from "$lib/components/mediaUploader.svelte";
    import ObjectAttributes from "$lib/components/objectAttributes.svelte";

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
        colorHex: '#ffffff', // Add hex value storage
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
    
    let errors = {
        title: '',
        media: '',
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
    
    // Handle updates from the MediaUploader component
    function handleMediaUpdate(event) {
        mediaFiles = event.detail.mediaFiles;
    }
    
    // Handle updates from ObjectAttributes component
    function handleAttributesUpdate(event) {
        activeAttributes = event.detail.activeAttributes;
    }
    
    // Handle attribute value changes
    function handleAttributeValueChange(event) {
        attributeValues = event.detail.attributeValues;
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
            
            // Deep clone and sanitize attribute values to prevent JSON issues
            const sanitizedAttributes = JSON.parse(JSON.stringify(attributeValues));
            
            // Create mysteryObject using sanitized attributes
            const mysteryObject = {
                description,
                material: sanitizedAttributes.material || null,
                // Fix: Only use the hex color value without additional properties
                color: sanitizedAttributes.colorHex || null,
                shape: sanitizedAttributes.shape || null,
                location: sanitizedAttributes.location || null,
                smell: sanitizedAttributes.smell || null,
                taste: sanitizedAttributes.taste || sanitizedAttributes.smell || null,
                texture: sanitizedAttributes.texture || null,
                functionality: sanitizedAttributes.functionality || null,
                markings: sanitizedAttributes.marking || null,
                handmade: sanitizedAttributes.handmade || false,
                oneOfAKind: sanitizedAttributes.oneOfAKind || false,
                weight: parseFloat(sanitizedAttributes.weight) || null,
                timePeriod: sanitizedAttributes.period || null,
                writtenText: sanitizedAttributes.writtenText || null,
                descriptionOfParts: sanitizedAttributes.descriptionOfParts || null,
                hardness: sanitizedAttributes.hardness || null,
                value: parseFloat(sanitizedAttributes.value) || null,
                originOfAcquisition: sanitizedAttributes.originOfAcquisition || null,
                pattern: sanitizedAttributes.pattern || null,
                brand: sanitizedAttributes.brand || null,
                print: sanitizedAttributes.print || null,
                imageLicenses: sanitizedAttributes.imageLicenses || null,
                sizeX: parseFloat(sanitizedAttributes.sizeX) || null,
                sizeY: parseFloat(sanitizedAttributes.sizeY) || null,
                sizeZ: parseFloat(sanitizedAttributes.sizeZ) || null,
                item_condition: sanitizedAttributes.itemCondition || null
            };
            
            // Store color name as a UI-side property that doesn't go to the backend
            // This will be handled separately by the Post component
            const colorNameForUI = attributeValues.color;
            
            // Create FormData with better error handling
            const formData = new FormData();
            formData.append('title', title);
            formData.append('content', description);
            
            // Add tags
            if (tags && tags.length > 0) {
                const tagIds = tags.map(tag => tag.id);
                formData.append('tags', JSON.stringify(tagIds));
            }
            
            try {
                // Cleanup the mysteryObject to ensure it's valid JSON
                // Remove any undefined, null, or empty string values to minimize issues
                Object.keys(mysteryObject).forEach(key => {
                    if (mysteryObject[key] === undefined || mysteryObject[key] === '' || mysteryObject[key] === null) {
                        delete mysteryObject[key];
                    }
                });
                
                // Safely stringify the mysteryObject
                const mysteryObjectJSON = JSON.stringify(mysteryObject);
                formData.append('mysteryObject', mysteryObjectJSON);
                
                // Log for debugging
                console.log('Sending mysteryObject:', mysteryObjectJSON);
            } catch (jsonError) {
                console.error('Error converting mysteryObject to JSON:', jsonError, mysteryObject);
                throw new Error('Failed to process object data');
            }
            
            // Add first media file as the primary image for backward compatibility
            if (mediaFiles.length > 0) {
                const firstFile = mediaFiles[0].file;
                formData.append('image', firstFile);
            }

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
            
            // Upload all media files (including the first one again to ensure it's in the new system)
            for (let i = 0; i < mediaFiles.length; i++) {
                const mediaItem = mediaFiles[i];
                const mediaFormData = new FormData();
                mediaFormData.append('file', mediaItem.file);
                mediaFormData.append('type', mediaItem.type || 'image');
                
                try {
                    const mediaResponse = await fetch(`${PUBLIC_API_URL}/api/mysteryObjects/${responseData.mysteryObjectId}/upload-media`, {
                        method: 'POST',
                        body: mediaFormData,
                        credentials: 'include'
                    });
            
                    if (!mediaResponse.ok) {
                        console.error(`Failed to upload media file ${i+1}`);
                        // Continue with other uploads even if one fails
                    }
                } catch (mediaError) {
                    console.error(`Error uploading media file ${i+1}:`, mediaError);
                    // Continue with other uploads
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

            <!-- Description -->
            <div class="mb-5">
                <label for="description" class="block text-sm font-medium mb-1.5">Description*</label>
                <Textarea id="description" class="w-full p-2 mb-4 border rounded dark:border-gray-600 h-32" bind:value={description} placeholder="You can add any additional context about your object or how you came into possession of it."/>
                {#if errors.description}
                    <p class="text-red-500 text-sm mt-1">{errors.description}</p>
                {/if}
            </div>
             
            <!-- Attributes and Tags wrapper for side-by-side on lg screens -->
            <div class="flex flex-col lg:flex-row lg:gap-6">
                <!-- Object Attributes Component -->
                <div class="w-full lg:w-1/2">
                    <ObjectAttributes 
                        bind:attributeValues={attributeValues}
                        bind:activeAttributes={activeAttributes}
                        on:update={handleAttributesUpdate}
                        on:valuechange={handleAttributeValueChange}
                    />
                </div>

                <!-- Tags -->
                <div class="w-full lg:w-1/2 mb-5">
                    <!-- svelte-ignore a11y-label-has-associated-control -->
                    <label class="lg:block text-sm font-medium mb-1.5 flex items-center">
                        <span>Tags</span>
                        <span class="ml-2 px-2 py-0.5 bg-gray-100 dark:bg-gray-800 text-xs rounded-full">
                            {tags.length} added
                        </span>
                    </label>
                    <Query bind:tags={tags} bind:labels={labels} />
                </div>
            </div>

            <!-- Media upload component -->
            <MediaUploader
                bind:mediaFiles={mediaFiles}
                bind:errors={errors}
                on:update={handleMediaUpdate}
            />

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
