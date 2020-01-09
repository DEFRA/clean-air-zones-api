#!/usr/bin/env ruby
require 'date'


#turns off warning
$VERBOSE = nil
@@NTR_API_FILE_NAME = 'taxi-10000-api-json.txt'
@@UPPER_ALPHABET_ARRAY = ('A'..'Z').to_a
@@LOWER_ALPHABET_ARRAY = ('a'..'z').to_a
@@NUMBERS_ARRAY = ('1'..'9').to_a
@@LICENSING_AUTHORITY_A = 'Leeds'
@@LICENSING_AUTHORITY_B = 'Leeds'


def vehicle_to_text_file(file, isLastItem, vrm, licenseStartDate, licenseEndDate, vehicleType, licensing_authority, reg, isWheelchairAccessible)
    file.puts'    {'
    file.puts'      "vrm": "' + vrm + '",'
    file.puts'      "start": "' + licenseStartDate.strftime("%Y") + '-' + licenseStartDate.strftime("%m") + '-' + licenseStartDate.strftime("%d") + '",'
    file.puts'      "end": "' + licenseEndDate.strftime("%Y") + '-' + licenseEndDate.strftime("%m") + '-' + licenseEndDate.strftime("%d") + '",'
    file.puts'      "taxiOrPHV": "' + vehicleType + '",'
    file.puts'      "licensingAuthorityName": "' + licensing_authority + '",'
    file.puts'      "licensePlateNumber": "' + reg + '",'
    file.puts'      "wheelchairAccessibleVehicle": ' + isWheelchairAccessible + ''
    if isLastItem == false
        file.puts'    },'
    else 
        file.puts'    }'  
    end  
end


def generate_ntr_vehicle
    vrm = make_valid_vrm
    reg = make_valid_reg
    vehicleType = ['taxi','PHV'].sample
    licensing_authority = [@@LICENSING_AUTHORITY_A,@@LICENSING_AUTHORITY_B].sample
    isWheelchairAccessible = ['true','false'].sample
    licenseStartDate = rand(Date.civil(2005, 01, 01)..Date.civil(2030, 12, 30))
    licenseEndDate = rand(licenseStartDate..Date.civil(2030, 12, 31))
    [vrm , licenseStartDate, licenseEndDate, vehicleType, licensing_authority, reg, isWheelchairAccessible]
end

def make_valid_vrm
    vrmp1 = @@UPPER_ALPHABET_ARRAY.shuffle[0,2].join
    vrmp2 = @@NUMBERS_ARRAY.shuffle[0,2].join
    vrmp3 = @@UPPER_ALPHABET_ARRAY.shuffle[0,3].join
    vrmp1 + vrmp2 + vrmp3
end


def make_valid_reg
    reg1 = @@LOWER_ALPHABET_ARRAY.shuffle[0,2].join
    reg2 = @@UPPER_ALPHABET_ARRAY.shuffle[0,3].join
    reg1 + reg2
end




def generate_ntr_api_file(numOfRows)
    puts 'Generating a file with ' + numOfRows.to_s + ' data items'
    if defined?(@@FILE_NAME) == nil
        @@FILE_NAME = @@NTR_API_FILE_NAME
    end
    puts 'Creating file ' + @@FILE_NAME
    File.open(@@FILE_NAME, "w") do |file|
        file.puts '{'
        file.puts '  "vehicleDetails": ['
        for i in 1..numOfRows do
            vehicle_data = generate_ntr_vehicle
            isLastItem = (i == numOfRows)
            vehicle_to_text_file(file, isLastItem, *vehicle_data)
        end
        file.puts'  ]'
        file.puts'}'
    end
    puts 'API file generated'
end


def generate_ntr_files(numOfRows)
    puts 'Generating a csv and a file with ' + numOfRows.to_s + ' data items'
    csv = CSV.open(@@NTR_CSV_FILE_NAME, "wb")
    file = File.open(@@NTR_API_FILE_NAME, "w")
    file.puts '{'
    file.puts '  "vehicleDetails": ['
    for i in 1..numOfRows do
        vrm = make_valid_vrm
        reg = make_valid_reg
        vehicleType = ['taxi','PHV'].sample
        licensing_authority = [@@LICENSING_AUTHORITY_A,@@LICENSING_AUTHORITY_B].sample
        isWheelchairAccessible = ['true','false'].sample
        licenseStartDate = rand(Date.civil(1885, 01, 01)..Date.civil(2100, 12, 30))
        licenseEndDate = rand(licenseStartDate..Date.civil(2100, 12, 31))
        csv << [vrm, licenseStartDate, licenseEndDate, vehicleType, licensing_authority, reg, isWheelchairAccessible]
        isLastItem = (i == numOfRows)
        vehicle_to_text_file(file, isLastItem, vrm, licenseStartDate, licenseEndDate, vehicleType, licensing_authority, reg, isWheelchairAccessible)
    end
    file.puts'  ]'
    file.puts'}'
    file.close
    csv.close
    remove_newline(@@NTR_CSV_FILE_NAME)
    puts 'CSV and API files generated'
end

generate_ntr_api_file(10000)
